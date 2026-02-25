package fr.ubordeaux.pdp.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for the three evaluation functions:
 *   - evaluateSimple()   : material only (pawns + checkers)
 *   - evaluateAdvanced() : material + advancement bonus
 *   - evaluateMax()      : material + advancement + center + mobility
 *
 * Convention: positive score = white advantage, negative = black advantage.
 */
class EvaluateTest {

  // ===========================================================================
  //  evaluateSimple() — matériel uniquement
  // ===========================================================================

  @Test
  void simple_emptyBoardIsZero() {
    Board board = new Board(12);
    clearBoard(board, 12);
    assertEquals(0, board.evaluateSimple(), "Empty board must be 0");
  }

  @Test
  void simple_initialPositionIsSymmetric() {
    assertEquals(0, new Board(8).evaluateSimple(),  "8x8 initial must be 0");
    assertEquals(0, new Board(10).evaluateSimple(), "10x10 initial must be 0");
    assertEquals(0, new Board(12).evaluateSimple(), "12x12 initial must be 0");
  }

  @Test
  void simple_onePawnWhite_positive() {
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addWhite("F6");
    assertTrue(board.evaluateSimple() > 0, "One white pawn must be positive");
  }

  @Test
  void simple_onePawnBlack_negative() {
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addBlack("G7");
    assertTrue(board.evaluateSimple() < 0, "One black pawn must be negative");
  }

  @Test
  void simple_checkerWorthMoreThanPawn() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("F6");
    int pawnScore = board.evaluateSimple();

    clearBoard(board, 12);
    board.addWhite("F6");
    board.promote("F6");
    int checkerScore = board.evaluateSimple();

    assertTrue(checkerScore > pawnScore, "Checker (350) must score more than pawn (100)");
  }

  @Test
  void simple_materialDifference_twoVsOne() {
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addWhite("C3");
    board.addWhite("E5");
    board.addBlack("H8");

    assertEquals(100, board.evaluateSimple(), "2 white pawns vs 1 black pawn = +100");
  }

  @Test
  void simple_locationDoesNotMatter() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("A1");
    int scoreCorner = board.evaluateSimple();

    clearBoard(board, 12);
    board.addWhite("F6");
    int scoreCenter = board.evaluateSimple();

    assertEquals(scoreCorner, scoreCenter,
        "evaluateSimple must not vary with position (no positional bonus)");
  }

  @Test
  void simple_signFlipWhenColorsReversed() {
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addWhite("C3");
    board.addWhite("E5");
    board.addBlack("H8");
    int white = board.evaluateSimple();

    clearBoard(board, 12);
    board.addBlack("C3");
    board.addBlack("E5");
    board.addWhite("H8");
    int black = board.evaluateSimple();

    assertEquals(-white, black, "Reversing colors must negate the score exactly");
  }

  // ===========================================================================
  //  evaluateAdvanced() — matériel + avancement
  // ===========================================================================

  @Test
  void advanced_emptyBoardIsZero() {
    Board board = new Board(12);
    clearBoard(board, 12);
    assertEquals(0, board.evaluateAdvanced(), "Empty board must be 0");
  }

  @Test
  void advanced_initialPositionIsSymmetric() {
    assertEquals(0, new Board(8).evaluateAdvanced(),  "8x8 initial must be 0");
    assertEquals(0, new Board(10).evaluateAdvanced(), "10x10 initial must be 0");
    assertEquals(0, new Board(12).evaluateAdvanced(), "12x12 initial must be 0");
  }

  @Test
  void advanced_whitePawnAdvancementIncreasesScore() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("A1"); // row 0, bonus = 0
    int scoreBack = board.evaluateAdvanced();

    clearBoard(board, 12);
    board.addWhite("K11"); // row 10, bonus = 10*8 = 80
    int scoreAdvanced = board.evaluateAdvanced();

    assertTrue(scoreAdvanced > scoreBack,
        "White pawn further north must score higher");
    assertEquals(80, scoreAdvanced - scoreBack,
        "Advancement bonus difference must be 80 (10 rows * 8)");
  }

  @Test
  void advanced_blackPawnAdvancementDecreasesScore() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addBlack("L12"); // row 11, bonus = (11-11)*8 = 0 — position de départ
    int scoreBack = board.evaluateAdvanced();

    clearBoard(board, 12);
    board.addBlack("B2"); // row 1, bonus = (11-1)*8 = 80 — proche du but
    int scoreAdvanced = board.evaluateAdvanced();

    assertTrue(scoreAdvanced < scoreBack,
        "Black pawn nearer its goal (B2) must score more negatively than at start (L12)");
    assertEquals(-80, scoreAdvanced - scoreBack,
        "Black advancement bonus difference must be -80");
  }

  @Test
  void advanced_strictlyMoreThanSimple_forAdvancedWhitePawn() {
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addWhite("K11"); // row 10 → bonus avancement non nul

    assertTrue(board.evaluateAdvanced() > board.evaluateSimple(),
        "evaluateAdvanced must exceed evaluateSimple for an advanced white pawn");
  }

  @Test
  void advanced_checkerNoAdvancementBonus() {
    // Les checkers n'ont pas de bonus d'avancement dans evaluateAdvanced
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("A1");
    board.promote("A1");
    int scoreBack = board.evaluateAdvanced();

    clearBoard(board, 12);
    board.addWhite("K11");
    board.promote("K11");
    int scoreAdvanced = board.evaluateAdvanced();

    assertEquals(scoreBack, scoreAdvanced,
        "Checkers must not receive advancement bonus in evaluateAdvanced");
  }

  // ===========================================================================
  //  evaluateMax() — matériel + avancement + centre + mobilité
  // ===========================================================================

  @Test
  void max_emptyBoardIsZero() {
    Board board = new Board(12);
    clearBoard(board, 12);
    assertEquals(0, board.evaluateMax(), "Empty board must be 0");
  }

  @Test
  void max_initialPositionIsSymmetric8x8() {
    assertEquals(0, new Board(8).evaluateMax(), "8x8 initial must be 0");
  }

  @Test
  void max_initialPositionIsSymmetric10x10() {
    assertEquals(0, new Board(10).evaluateMax(), "10x10 initial must be 0");
  }

  @Test
  void max_initialPositionIsSymmetric12x12() {
    assertEquals(0, new Board(12).evaluateMax(),
        "12x12 initial must be 0 after center fix (sizeBoard/2 ± 2)");
  }

  @Test
  void max_whitePawnInCenterScoresMoreThanEdge() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("A1");
    int scoreEdge = board.evaluateMax();

    clearBoard(board, 12);
    board.addWhite("F6");
    int scoreCenter = board.evaluateMax();

    assertTrue(scoreCenter > scoreEdge,
        "White pawn in center must score higher than on edge");
  }

  @Test
  void max_blackPawnInCenterScoresMoreNegativeThanEdge() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addBlack("L12");
    int scoreEdge = board.evaluateMax();

    clearBoard(board, 12);
    board.addBlack("G7");
    int scoreCenter = board.evaluateMax();

    assertTrue(scoreCenter < scoreEdge,
        "Black pawn in center must score more negatively than on edge");
  }

  @Test
  void max_mobilityAddsScore_moreMovesIsBetter() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("A1"); // coin, 1 seul mouvement possible
    int scoreLow = board.evaluateMax();

    clearBoard(board, 12);
    board.addWhite("E5");
    board.addWhite("G5");
    board.addWhite("C5"); // 3 pions au centre = plus de mobilité
    int scoreHigh = board.evaluateMax();

    assertTrue(scoreHigh > scoreLow,
        "More white pieces with more mobility must score higher");
  }

  @Test
  void max_strictlyMoreThanAdvanced_forCentralPawn() {
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addWhite("F6"); // case centrale

    assertTrue(board.evaluateMax() > board.evaluateAdvanced(),
        "evaluateMax must exceed evaluateAdvanced for a central pawn (center + mobility bonus)");
  }

  @Test
  void max_signFlipWhenColorsReversed() {
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addWhite("C3");
    board.addWhite("E5");
    board.addBlack("H8");
    int white = board.evaluateMax();

    clearBoard(board, 12);
    board.addBlack("C3");
    board.addBlack("E5");
    board.addWhite("H8");
    int black = board.evaluateMax();

    assertTrue(white > 0 && black < 0,
        "Reversing colors must flip the sign of evaluateMax");
  }

  @Test
  void max_checkerVsPawnAdvantageIsSignificant() {
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addWhite("F6");
    board.promote("F6");
    board.addBlack("G7");

    assertTrue(board.evaluateMax() > 100,
        "White checker vs black pawn must yield a score > 100");
  }

  // ===========================================================================
  //  Comparaison entre les 3 fonctions
  // ===========================================================================

  @Test
  void comparison_simpleIgnoresPosition() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("A1");
    int s1 = board.evaluateSimple();
    clearBoard(board, 12);
    board.addWhite("L12");
    int s2 = board.evaluateSimple();

    assertEquals(s1, s2,
        "evaluateSimple must return the same score regardless of position");
  }

  @Test
  void comparison_advancedDiffersFromSimpleWhenPawnAdvanced() {
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addWhite("K11");

    assertNotEquals(board.evaluateSimple(), board.evaluateAdvanced(),
        "evaluateAdvanced must differ from evaluateSimple for an advanced pawn");
  }

  @Test
  void comparison_maxDiffersFromAdvancedForCentralPiece() {
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addWhite("F6");

    assertNotEquals(board.evaluateAdvanced(), board.evaluateMax(),
        "evaluateMax must differ from evaluateAdvanced (center/mobility bonus)");
  }

  @Test
  void comparison_allThreeEqualOnEmptyBoard() {
    Board board = new Board(12);
    clearBoard(board, 12);

    assertEquals(0, board.evaluateSimple());
    assertEquals(0, board.evaluateAdvanced());
    assertEquals(0, board.evaluateMax());
  }

  @Test
  void comparison_rankingSimpleLessAdvancedLessMax_forGoodPosition() {
    // Un pion blanc en case centrale avancée doit donner :
    // simple < advanced < max (les bonus s'accumulent)
    Board board = new Board(12);
    clearBoard(board, 12);
    board.addWhite("F6"); // rangée 5 (avancée) + case centrale

    int s = board.evaluateSimple();
    int a = board.evaluateAdvanced();
    int m = board.evaluateMax();

    assertTrue(s <= a, "evaluateAdvanced must be >= evaluateSimple for an advanced pawn");
    assertTrue(a <= m, "evaluateMax must be >= evaluateAdvanced for a central pawn");
    assertTrue(s < m,  "evaluateMax must strictly exceed evaluateSimple");
  }

  // ===========================================================================
  //  Helper
  // ===========================================================================

  private void clearBoard(Board board, int size) {
    for (int row = 1; row <= size; row++) {
      for (int col = 0; col < size; col++) {
        if ((row + col) % 2 != 0) {
          char file = (char) ('A' + col);
          String square = file + "" + row;
          board.remove(square);
        }
      }
    }
  }
}