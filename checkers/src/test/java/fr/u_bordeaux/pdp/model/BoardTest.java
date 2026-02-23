package fr.u_bordeaux.pdp.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class BoardTest {

  // ----------------------------
  // CONSTRUCTEUR
  // ----------------------------

  @Test
  void testInvalidBoardSize() {
    assertThrows(IllegalArgumentException.class, () -> new Board(6));
    assertThrows(IllegalArgumentException.class, () -> new Board(9));
  }

  @Test
  void testValidBoardSizes() {
    assertDoesNotThrow(() -> new Board(8));
    assertDoesNotThrow(() -> new Board(10));
    assertDoesNotThrow(() -> new Board(12));
  }

  // ----------------------------
  // INITIAL POSITION (8x8)
  // ----------------------------

  @Test
  void testInitialWhiteAndBlackPresence8x8() {
    Board board = new Board(8);

    assertTrue(board.isWhitePawn("A1"));
    assertTrue(board.isWhitePawn("C7"));

    assertTrue(board.isBlackPawn("H2"));
    assertTrue(board.isBlackPawn("F8"));

    assertFalse(board.isWhitePawn("E1"));
    assertFalse(board.isWhitePawn("D8"));
  }

  // ----------------------------
  // INITIAL POSITION (12x12)
  // ----------------------------

  @Test
  void testIndexLimite() {
    Board board = new Board(12);
    assertThrows(IllegalArgumentException.class, () -> board.isBlackPawn("I13"));
    assertThrows(IllegalArgumentException.class, () -> board.isBlackPawn("M1"));
  }

  @Test
  void testInitialWhiteAndBlackPresence12x12() {
    Board board = new Board(12);

    assertTrue(board.isWhitePawn("A1"));
    assertTrue(board.isWhitePawn("E11"));

    assertTrue(board.isBlackPawn("L12"));
    assertTrue(board.isBlackPawn("H2"));

    assertTrue(board.occupied("H2"));
    assertTrue(board.occupied("E11"));
    assertFalse(board.occupied("G1"));
    assertFalse(board.occupied("F12"));
  }

  // ----------------------------
  // CONVERSION INDEX <-> SQUARE
  // ----------------------------

  @Test
  void testSquareToIndexAndBack() {
    Board board = new Board(8);

    int index = board.squareToIndex("A1");
    String square = board.indexToSquare(index);

    assertEquals("A1", square);
  }

  @Test
  void testWhiteSquareThrowsException() {
    Board board = new Board(8);

    assertThrows(IllegalArgumentException.class, () -> board.squareToIndex("A2"));
  }

  // ----------------------------
  // OCCUPIED
  // ----------------------------

  @Test
  void testOccupied() {
    Board board = new Board(12);

    assertTrue(board.occupied("A1"));
    assertTrue(board.occupied("L12"));
    assertFalse(board.occupied("G5"));

    int from = board.squareToIndex("E1");
    int to = board.squareToIndex("L2");
    Move move = new Move(List.of(from, to), List.of());
    board.applyMove(move);
    assertTrue(board.occupied("L2"));

    from = board.squareToIndex("H2");
    to = board.squareToIndex("A9");
    move = new Move(List.of(from, to), List.of());
    board.applyMove(move);
    assertTrue(board.occupied("A9"));
  }

  // ----------------------------
  // PROMOTION
  // ----------------------------

  @Test
  void testPromotionWhiteAndBlack() {
    Board board = new Board(8);

    int whiteFrom = board.squareToIndex("C3");
    int whiteTo = board.squareToIndex("H8");
    Move whiteMove = new Move(List.of(whiteFrom, whiteTo), List.of());

    board.applyMove(whiteMove);

    assertTrue(board.isWhiteChecker("H8"), "H8 doit contenir la reine blanche après promotion");
    assertTrue(whiteMove.isPromotion(), "Le move blanc doit être marqué comme promotion");

    int blackFrom = board.squareToIndex("F6");
    int blackTo = board.squareToIndex("A1");
    Move blackMove = new Move(List.of(blackFrom, blackTo), List.of());

    board.applyMove(blackMove);
  }

  // ----------------------------
  // APPLY MOVE SIMPLE
  // ----------------------------

  @Test
  void testSimpleMovesWhiteAndBlackPawn() {
    Board board = new Board(12);

    int whiteFrom = board.squareToIndex("C3");
    int whiteTo = board.squareToIndex("D4");
    Move whiteMove = new Move(List.of(whiteFrom, whiteTo), List.of());

    board.applyMove(whiteMove);

    assertFalse(board.isWhitePawn("C3"), "La case C3 ne doit plus contenir le pion blanc");
    assertTrue(board.isWhitePawn("D4"), "La case D4 doit contenir le pion blanc");

    int blackFrom = board.squareToIndex("H8");
    int blackTo = board.squareToIndex("G7");
    Move blackMove = new Move(List.of(blackFrom, blackTo), List.of());

    board.applyMove(blackMove);

    assertFalse(board.isBlackPawn("H8"), "La case H8 ne doit plus contenir le pion noir");
    assertTrue(board.isBlackPawn("G7"), "La case G7 doit contenir le pion noir");
  }

  @Test
  void testCheckerMoveWhiteAndBlack() {
    Board board = new Board(12);

    int whitePawn = board.squareToIndex("C3");
    board.promote("C3");
    assertTrue(board.isWhiteChecker("C3"), "C3 doit être une reine blanche après promotion");

    int whiteTo = board.squareToIndex("F6");
    Move whiteMove = new Move(List.of(whitePawn, whiteTo), List.of());
    board.applyMove(whiteMove);

    assertFalse(board.isWhiteChecker("C3"), "C3 doit être vide après le déplacement");
    assertTrue(board.isWhiteChecker("F6"), "F6 doit contenir la reine blanche");

    int blackPawn = board.squareToIndex("H8");
    board.promote("H8");
    assertTrue(board.isBlackChecker("H8"), "H8 doit être une reine noire après promotion");

    int blackTo = board.squareToIndex("E5");
    Move blackMove = new Move(List.of(blackPawn, blackTo), List.of());
    board.applyMove(blackMove);

    assertFalse(board.isBlackChecker("H8"), "H8 doit être vide après le déplacement");
    assertTrue(board.isBlackChecker("E5"), "E5 doit contenir la reine noire");
  }

  @Test
  void testCheckerSimpleTargetsBlocked() {
    Board board = new Board(12);

    int blockingSquare = board.squareToIndex("G7");

    // Block the square with a white pawn
    board.addWhite("G7");

    List<Integer> targets = board.checkerSimpleTarg("F6");

    assertFalse(
        targets.contains(blockingSquare),
        "Occupied square must not be included in checker simple targets");
  }

  @Test
  void testCheckerSimpleTargetsBord() {
    Board board = new Board(12);

    int blockingSquare = board.squareToIndex("G1");

    // Block the square with a white pawn
    board.addWhite("G1");

    List<Integer> targets = board.checkerSimpleTarg("H2");

    assertFalse(
        targets.contains(blockingSquare),
        "Occupied border square must not be included in checker simple targets");
  }

  @Test
  void testCheckerCaptureWhiteAndBlack() {
    Board board = new Board(12);

    // --- White checker capture ---
    board.promote("C3");
    assertTrue(board.isWhiteChecker("C3"));

    board.addBlack("D4");
    assertTrue(board.isBlackPawn("D4"));

    int fromWhite = board.squareToIndex("C3");
    int toWhite = board.squareToIndex("E5");

    Move whiteCapture =
        new Move(List.of(fromWhite, toWhite),
            List.of(board.squareToIndex("D4")));
    board.applyMove(whiteCapture);

    assertFalse(board.isWhiteChecker("C3"),
        "C3 must be empty after move");
    assertTrue(board.isWhiteChecker("E5"),
        "E5 must contain the white checker");
    assertFalse(board.isBlackPawn("D4"),
        "D4 must be empty after capture");

    // --- Black checker capture ---
    board.promote("H8");
    assertTrue(board.isBlackChecker("H8"));

    board.addWhite("G7");
    assertTrue(board.isWhitePawn("G7"));

    int fromBlack = board.squareToIndex("H8");
    int toBlack = board.squareToIndex("F6");

    Move blackCapture =
        new Move(List.of(fromBlack, toBlack),
            List.of(board.squareToIndex("G7")));
    board.applyMove(blackCapture);

    assertFalse(board.isBlackChecker("H8"),
        "H8 must be empty after move");
    assertTrue(board.isBlackChecker("F6"),
        "F6 must contain the black checker");
    assertFalse(board.isWhitePawn("G7"),
        "G7 must be empty after capture");
  }

  // ----------------------------
  // VALID MOVES
  // ----------------------------

  @Test
  void testWhiteHasValidMovesAtStart() {
    Board board = new Board(12);

    List<Move> moves = board.getWhiteValidMoves();

    assertFalse(moves.isEmpty());
  }

  @Test
  void testBlackHasValidMovesAtStart() {
    Board board = new Board(8);

    List<Move> moves = board.getBlackValidMoves();

    assertFalse(moves.isEmpty());
  }

  // ----------------------------
  // LIMITS
  // ----------------------------

  @Test
  void testOutOfBoundsPromotion() {
    Board board = new Board(8);

    assertThrows(IllegalArgumentException.class, () -> board.promote("Z9"));
  }

  @Test
  void testNoPiecesLeft() {
    Board board = new Board(12);
    assertFalse(board.noPiecesLeft(false));
    assertFalse(board.noPiecesLeft(true));
  }

  // ----------------------------
  // PAWN MULTI-CAPTURE
  // ----------------------------

  @Test
  void testWhitePawnMultiCapture12x12() {
    Board board = new Board(12);

    clearBoard(board, 12);

    board.addWhite("C3");

    board.addBlack("D4");
    board.addBlack("F6");

    List<Move> moves = board.getWhiteValidMoves();
    List<Move> captures =
        moves.stream().filter(Move::isCapture).toList();

    assertFalse(captures.isEmpty(),
        "White pawn should have a multiple capture");

    boolean foundBlack1 = false;
    boolean foundBlack2 = false;

    int black1 = board.squareToIndex("D4");
    int black2 = board.squareToIndex("F6");

    for (Move move : captures) {
      for (int captured : move.getCaptured()) {
        if (captured == black1) {
          foundBlack1 = true;
        }
        if (captured == black2) {
          foundBlack2 = true;
        }
      }
    }

    assertTrue(foundBlack1,
        "White pawn must capture black pawn on D4");
    assertTrue(foundBlack2,
        "White pawn must capture black pawn on F6");
  }

  // ----------------------------
  // CHECKER MULTI-CAPTURE
  // ----------------------------

  @Test
  public void testWhiteCheckerMulti12x12() {
    Board board = new Board(12);

    // Nettoyer le plateau
    clearBoard(board, 12);

    // Ajouter un checker blanc
    board.addWhite("C3");

    // Ajouter des pions noirs à capturer
    board.addBlack("D4");
    board.addBlack("F6");

    // Récupérer tous les coups valides pour les blancs
    List<Move> moves = board.getWhiteValidMoves();
    List<Move> captures = moves.stream()
                              .filter(Move::isCapture)
                              .toList();

    assertFalse(captures.isEmpty(), "Le checker blanc devrait avoir au moins une capture");

    boolean foundBlack1 = false;
    boolean foundBlack2 = false;

    for (Move move : captures) {
      for (int captured : move.getCaptured()) {
        if (captured == board.squareToIndex("D4")) foundBlack1 = true;
        if (captured == board.squareToIndex("F6")) foundBlack2 = true;
      }
    }

    assertTrue(foundBlack1, "Le checker doit capturer le pion noir en D4");
    assertTrue(foundBlack2, "Le checker doit capturer le pion noir en F6");
  }

  @Test
  public void testBlackCheckerMultiCapture12x12() {
    Board board = new Board(12);

    // Nettoyer le plateau
    clearBoard(board, 12);

    // Ajouter un checker noir
    board.addBlack("H8");

    // Ajouter des pions et un checker blancs à capturer
    board.addWhite("G7"); // pion
    board.addWhite("E5"); // pion
    board.addWhite("C3"); // checker

    // Récupérer tous les coups valides pour les noirs
    List<Move> moves = board.getBlackValidMoves();
    List<Move> captures = moves.stream()
                              .filter(Move::isCapture)
                              .toList();

    assertFalse(captures.isEmpty(), "Le checker noir doit avoir au moins une capture");

    boolean capturedPawn1 = false;
    boolean capturedPawn2 = false;
    boolean capturedChecker = false;

    for (Move move : captures) {
      for (int captured : move.getCaptured()) {
        if (captured == board.squareToIndex("G7")) capturedPawn1 = true;
        if (captured == board.squareToIndex("E5")) capturedPawn2 = true;
        if (captured == board.squareToIndex("C3")) capturedChecker = true;
      }
    }

    assertTrue(capturedPawn1, "Le checker noir doit capturer le pion blanc en G7");
    assertTrue(capturedPawn2, "Le checker noir doit capturer le pion blanc en E5");
    assertTrue(capturedChecker, "Le checker noir doit capturer le checker blanc en C3");
  }

  // ----------------------------
  // HELPER
  // ----------------------------

  /** Removes all pieces from all squares via reflection. */
  private void clearBoard(Board board, int size) {
    for (int row = 1; row <= size; row++) {
      for (int col = 0; col < size; col++) {
        if ((row + col) % 2 != 0) { // seulement les cases jouables
          char file = (char) ('A' + col);
          String square = file + "" + row;
          board.remove(square);
        }
      }
    }
  }
}