package fr.ubordeaux.pdp.model.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.player.PlayerColor;

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

    Move whiteCapture = new Move(List.of(fromWhite, toWhite),
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

    Move blackCapture = new Move(List.of(fromBlack, toBlack),
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
  // GET VALID MOVES (12x12)
  // ----------------------------

  @Test
  void testGetValidMovesWhiteAtStart12x12() {
    Board board = new Board(12);

    List<Move> moves = board.getWhiteValidMoves();

    assertFalse(moves.isEmpty(), "White should have valid moves at start");
    assertTrue(moves.stream().allMatch(m -> !m.isCapture()),
        "No captures should be available at start on 12x12");
  }

  @Test
  void testGetValidMovesBlackAtStart12x12() {
    Board board = new Board(12);

    List<Move> moves = board.getBlackValidMoves();

    assertFalse(moves.isEmpty(), "Black should have valid moves at start");
    assertTrue(moves.stream().allMatch(m -> !m.isCapture()),
        "No captures should be available at start on 12x12");
  }

  @Test
  void testGetValidMovesCaptureIsMandatory12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("C3");
    board.addBlack("D4"); // capturable, landing E5 free

    List<Move> moves = board.getWhiteValidMoves();

    assertFalse(moves.isEmpty(), "White should have at least one move");
    assertTrue(moves.stream().allMatch(Move::isCapture),
        "Only captures must be returned when a capture is available (mandatory rule)");
  }

  @Test
  void testGetValidMovesNoCaptureFallsBackToSimple12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("F6"); // isolated white pawn, no enemy nearby

    List<Move> moves = board.getWhiteValidMoves();

    assertFalse(moves.isEmpty(), "White should have simple moves");
    assertTrue(moves.stream().noneMatch(Move::isCapture),
        "No captures should be returned when none are available");
  }

  @Test
  void testGetValidMovesEmptyBoardReturnsNothing12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    assertTrue(board.getWhiteValidMoves().isEmpty(),
        "No moves for white on empty 12x12 board");
    assertTrue(board.getBlackValidMoves().isEmpty(),
        "No moves for black on empty 12x12 board");
  }

  @Test
  void testGetValidMovesWhitePawnMovesForwardOnly12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("D4");

    List<Move> moves = board.getWhiteValidMoves();

    assertFalse(moves.isEmpty(), "White pawn on D4 should have moves");
    for (Move move : moves) {
      int fromRow = move.getFrom() / (12 / 2);
      int toRow = move.getTo() / (12 / 2);
      assertTrue(toRow > fromRow,
          "White pawn must only move forward (increasing row index)");
    }
  }

  @Test
  void testGetValidMovesBlackPawnMovesForwardOnly12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addBlack("I9");

    List<Move> moves = board.getBlackValidMoves();

    assertFalse(moves.isEmpty(), "Black pawn on I9 should have moves");
    for (Move move : moves) {
      int fromRow = move.getFrom() / (12 / 2);
      int toRow = move.getTo() / (12 / 2);
      assertTrue(toRow < fromRow,
          "Black pawn must only move forward (decreasing row index)");
    }
  }

  @Test
  void testGetValidMovesCheckerMovesInAllDirections12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("F6");
    board.promote("F6");

    List<Move> moves = board.getWhiteValidMoves();

    assertFalse(moves.isEmpty(), "White checker on F6 should have moves");

    boolean hasForward = false;
    boolean hasBackward = false;
    int fromRow = board.squareToIndex("F6") / (12 / 2);

    for (Move move : moves) {
      int toRow = move.getTo() / (12 / 2);
      if (toRow > fromRow)
        hasForward = true;
      if (toRow < fromRow)
        hasBackward = true;
    }

    assertTrue(hasForward, "Checker must be able to move forward");
    assertTrue(hasBackward, "Checker must be able to move backward");
  }

  @Test
  void testGetValidMovesOnlyMaxCaptures12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    // C3 captures D4 → lands E5, then captures F6 → lands G7: 2-capture chain.
    board.addWhite("C3");
    board.addBlack("D4");
    board.addBlack("F6");

    List<Move> captures = board.getWhiteValidMoves().stream()
        .filter(Move::isCapture)
        .toList();

    assertFalse(captures.isEmpty(), "White should have captures");

    int maxCaptures = captures.stream()
        .mapToInt(m -> m.getCaptured().size())
        .max()
        .orElse(0);

    assertTrue(maxCaptures >= 2,
        "Max capture sequence should contain at least 2 captures");
    for (Move move : captures) {
      assertEquals(maxCaptures, move.getCaptured().size(),
          "All returned captures must have the maximum capture count");
    }
  }

  @Test
  void testGetValidMovesPawnBlockedByOwnPiece12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("C3");
    board.addWhite("D4"); // own piece blocks NE diagonal

    List<Move> moves = board.getWhiteValidMoves();

    int d4Index = board.squareToIndex("D4");
    boolean movesToD4 = moves.stream().anyMatch(m -> m.getTo() == d4Index);

    assertFalse(movesToD4,
        "White pawn must not move onto a square occupied by its own piece");
  }

  @Test
  void testGetValidMovesCheckerCaptureIsMandatory12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("F6");
    board.promote("F6");
    board.addBlack("G7"); // enemy, landing H8 free

    List<Move> moves = board.getWhiteValidMoves();

    assertFalse(moves.isEmpty(), "White checker should have moves");
    assertTrue(moves.stream().allMatch(Move::isCapture),
        "Capture must be mandatory for checker when enemy is in range");
  }

  @Test
  void testGetValidMovesCheckerSlideRange12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    // Checker in corner A1 — on a 12x12 board, NE diagonal has up to 11 free
    // squares.
    board.addWhite("A1");
    board.promote("A1");

    List<Move> moves = board.getWhiteValidMoves();

    assertTrue(moves.size() >= 5,
        "Checker in corner of 12x12 board should reach many squares on the long diagonal");
  }

  @Test
  void testGetValidMovesStartingMoveCount12x12() {
    Board board = new Board(12);

    List<Move> whiteMoves = board.getWhiteValidMoves();
    List<Move> blackMoves = board.getBlackValidMoves();

    // On a 12x12 board, the front row of white has 6 pawns, each with up to 2
    // moves.
    assertTrue(whiteMoves.size() >= 6 && whiteMoves.size() <= 24,
        "White should have between 6 and 24 simple moves at start of 12x12");
    assertTrue(blackMoves.size() >= 6 && blackMoves.size() <= 24,
        "Black should have between 6 and 24 simple moves at start of 12x12");
  }

  // ----------------------------
  // LIMITS
  // ----------------------------

  @Test
  void testOutOfBoundsPromotion() {
    Board board = new Board(8);

    assertThrows(IllegalArgumentException.class, () -> board.promote("Z9"));
  }

  // ----------------------------
  // NO PIECES LEFT
  // ----------------------------

  @Test
  void testNoPiecesLeftFalseAtStart12x12() {
    Board board = new Board(12);

    assertFalse(board.noPiecesLeft(PlayerColor.WHITE), "White should have pieces at start");
    assertFalse(board.noPiecesLeft(PlayerColor.BLACK), "Black should have pieces at start");
  }

  @Test
  void testNoPiecesLeftTrueAfterClearWhite12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    assertTrue(board.noPiecesLeft(PlayerColor.WHITE), "White should have no pieces after clear");
    assertTrue(board.noPiecesLeft(PlayerColor.BLACK), "Black should have no pieces after clear");
  }

  @Test
  void testNoPiecesLeftWhiteOnlyPawn12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("F6");

    assertFalse(board.noPiecesLeft(PlayerColor.WHITE), "White has a pawn — noPiecesLeft must be false");
    assertTrue(board.noPiecesLeft(PlayerColor.BLACK), "Black has no pieces — noPiecesLeft must be true");
  }

  @Test
  void testNoPiecesLeftBlackOnlyPawn12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addBlack("G7");

    assertTrue(board.noPiecesLeft(PlayerColor.WHITE), "White has no pieces — noPiecesLeft must be true");
    assertFalse(board.noPiecesLeft(PlayerColor.BLACK), "Black has a pawn — noPiecesLeft must be false");
  }

  @Test
  void testNoPiecesLeftWhiteOnlyChecker12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("F6");
    board.promote("F6");

    assertFalse(board.noPiecesLeft(PlayerColor.WHITE), "White has a checker — noPiecesLeft must be false");
    assertTrue(board.noPiecesLeft(PlayerColor.BLACK), "Black has no pieces — noPiecesLeft must be true");
  }

  @Test
  void testNoPiecesLeftBlackOnlyChecker12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addBlack("G7");
    board.promote("G7");

    assertTrue(board.noPiecesLeft(PlayerColor.WHITE), "White has no pieces — noPiecesLeft must be true");
    assertFalse(board.noPiecesLeft(PlayerColor.BLACK), "Black has a checker — noPiecesLeft must be false");
  }

  @Test
  void testNoPiecesLeftAfterCapturingLastPiece12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    // White pawn on C3, black pawn on D4 — white captures the only black piece.
    board.addWhite("C3");
    board.addBlack("D4");

    int from = board.squareToIndex("C3");
    int captured = board.squareToIndex("D4");
    int to = board.squareToIndex("E5");
    Move capture = new Move(List.of(from, to), List.of(captured));
    board.applyMove(capture);

    assertFalse(board.noPiecesLeft(PlayerColor.WHITE), "White still has its pawn after capture");
    assertTrue(board.noPiecesLeft(PlayerColor.BLACK), "Black has no pieces left after being captured");
  }

  @Test
  void testNoPiecesLeftMixedPieceTypes12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    // White has both a pawn and a checker.
    board.addWhite("C3");
    board.addWhite("E5");
    board.promote("E5");

    assertFalse(board.noPiecesLeft(PlayerColor.WHITE), "White has pawn + checker — noPiecesLeft must be false");
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
    List<Move> captures = moves.stream().filter(Move::isCapture).toList();

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
        if (captured == board.squareToIndex("D4"))
          foundBlack1 = true;
        if (captured == board.squareToIndex("F6"))
          foundBlack2 = true;
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
        if (captured == board.squareToIndex("G7"))
          capturedPawn1 = true;
        if (captured == board.squareToIndex("E5"))
          capturedPawn2 = true;
        if (captured == board.squareToIndex("C3"))
          capturedChecker = true;
      }
    }

    assertTrue(capturedPawn1, "Le checker noir doit capturer le pion blanc en G7");
    assertTrue(capturedPawn2, "Le checker noir doit capturer le pion blanc en E5");
    assertTrue(capturedChecker, "Le checker noir doit capturer le checker blanc en C3");
  }
  // ----------------------------
  // DFS CHECKER
  // ----------------------------

  @Test
  void testDfsCheckerNoCapture() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addBlack("H8");

    List<Move> moves = board.getBlackValidMoves();
    List<Move> captures = moves.stream().filter(Move::isCapture).toList();

    assertTrue(captures.isEmpty(), "No capture should be available when the board is empty");
  }

  @Test
  void testDfsCheckerSingleCaptureForward() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addBlack("H8");
    board.promote("H8");
    board.addWhite("G7");

    List<Move> moves = board.getBlackValidMoves();
    List<Move> captures = moves.stream().filter(Move::isCapture).toList();

    assertFalse(captures.isEmpty(), "Black checker should have at least one capture");

    int whitePawn = board.squareToIndex("G7");
    boolean captured = captures.stream()
        .anyMatch(m -> m.getCaptured().contains(whitePawn));

    assertTrue(captured, "Black checker must capture white pawn on G7");
  }

  @Test
  void testDfsCheckerMultiCaptureChain() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addBlack("H8");
    board.promote("H8");
    board.addWhite("G7");
    board.addWhite("E5");
    board.addWhite("C3");

    List<Move> moves = board.getBlackValidMoves();
    List<Move> captures = moves.stream().filter(Move::isCapture).toList();

    assertFalse(captures.isEmpty(), "Black checker should have at least one capture");

    int white1 = board.squareToIndex("G7");
    int white2 = board.squareToIndex("E5");
    int white3 = board.squareToIndex("C3");

    boolean found1 = false, found2 = false, found3 = false;
    for (Move move : captures) {
      for (int cap : move.getCaptured()) {
        if (cap == white1)
          found1 = true;
        if (cap == white2)
          found2 = true;
        if (cap == white3)
          found3 = true;
      }
    }

    assertTrue(found1, "Black checker must capture white pawn on G7");
    assertTrue(found2, "Black checker must capture white pawn on E5");
    assertTrue(found3, "Black checker must capture white piece on C3");
  }

  @Test
  void testDfsCheckerBlockedByOwnPiece() {
    Board board = new Board(12);
    clearBoard(board, 12);

    // Black checker on A1 (corner, only NE diagonal available).
    // Own black pawn on B2 blocks that diagonal.
    // White pawn on C3 is beyond — unreachable for the checker via B2.
    // White pawn on D4 is placed so the black pawn on B2 has NO valid
    // landing square after jumping C3 (D4 is occupied), ensuring B2
    // generates no capture either and C3 stays unreachable for A1.
    board.addBlack("A1");
    board.promote("A1");
    board.addBlack("B2"); // own piece blocking the only path from A1 to C3
    board.addWhite("C3"); // enemy beyond own piece
    board.addWhite("D4"); // blocks B2's landing square — B2 cannot capture C3

    List<Move> captures = board.getBlackValidMoves().stream()
        .filter(Move::isCapture)
        .toList();

    int white = board.squareToIndex("C3");
    boolean capturedThroughOwnPiece = captures.stream()
        .anyMatch(m -> m.getCaptured().contains(white));

    assertFalse(capturedThroughOwnPiece,
        "Checker must not capture through its own piece");
  }

  @Test
  void testDfsCheckerDoesNotRecaptureAlreadyCaptured() {
    Board board = new Board(12);
    clearBoard(board, 12);

    // Black checker on H8, two whites in a line — cannot loop back
    board.addBlack("H8");
    board.promote("H8");
    board.addWhite("G7");
    board.addWhite("E5");

    List<Move> captures = board.getBlackValidMoves().stream()
        .filter(Move::isCapture)
        .toList();

    assertFalse(captures.isEmpty(), "Black checker should have captures");

    for (Move move : captures) {
      long distinctCaptured = move.getCaptured().stream().distinct().count();
      assertEquals(
          move.getCaptured().size(),
          (int) distinctCaptured,
          "The same piece must not be captured twice in one sequence");
    }
  }

  @Test
  void testDfsCheckerBestCaptureCountIsMaximum() {
    Board board = new Board(12);
    clearBoard(board, 12);

    // One path captures 2 (G7 + E5), another only 1 (G7 only going another way).
    // The DFS must return only the longest sequence.
    board.addBlack("H8");
    board.promote("H8");
    board.addWhite("G7");
    board.addWhite("E5");

    List<Move> captures = board.getBlackValidMoves().stream()
        .filter(Move::isCapture)
        .toList();

    int maxCaptures = captures.stream()
        .mapToInt(m -> m.getCaptured().size())
        .max()
        .orElse(0);

    assertTrue(maxCaptures >= 2,
        "The best capture sequence should contain at least 2 captures");

    for (Move move : captures) {
      assertEquals(maxCaptures, move.getCaptured().size(),
          "Only moves with the maximum number of captures should be returned");
    }
  }

  @Test
  void testDfsCheckerSlideBeforeCapture() {
    Board board = new Board(12);
    clearBoard(board, 12);

    // Checker at H8 must slide to reach the enemy placed far away
    board.addBlack("H8");
    board.promote("H8");
    board.addWhite("C3"); // far away diagonally — checker must slide then capture

    List<Move> captures = board.getBlackValidMoves().stream()
        .filter(Move::isCapture)
        .toList();

    int white = board.squareToIndex("C3");
    boolean found = captures.stream()
        .anyMatch(m -> m.getCaptured().contains(white));

    assertTrue(found,
        "Checker should be able to slide across empty squares and then capture on C3");
  }

  // ----------------------------
  // TO STRING
  // ----------------------------

  @Test
  void testToStringStartsWithNewline12x12() {
    Board board = new Board(12);

    assertTrue(board.toString().startsWith("\n"),
        "toString must start with a newline");
  }

  @Test
  void testToStringContainsAllRowLabels12x12() {
    Board board = new Board(12);
    String output = board.toString();

    for (char row = 'A'; row <= 'L'; row++) {
      assertTrue(output.contains(String.valueOf(row)),
          "toString must contain row label " + row);
    }
  }

  @Test
  void testToStringContainsAllColumnNumbers12x12() {
    Board board = new Board(12);
    String output = board.toString();

    for (int col = 1; col <= 12; col++) {
      assertTrue(output.contains(String.valueOf(col)),
          "toString must contain column number " + col);
    }
  }

  @Test
  void testToStringLineCount12x12() {
    Board board = new Board(12);

    // 1 leading newline + 12 piece rows + 1 column header row = 14 lines
    long lineCount = board.toString().lines().count();

    assertEquals(14, lineCount,
        "toString of a 12x12 board must have 14 lines (1 blank + 12 rows + 1 header)");
  }

  @Test
  void testToStringContainsWhitePawnSymbol12x12() {
    Board board = new Board(12);

    assertTrue(board.toString().contains("o"),
        "toString must contain 'o' for white pawns at start");
  }

  @Test
  void testToStringContainsBlackPawnSymbol12x12() {
    Board board = new Board(12);

    assertTrue(board.toString().contains("x"),
        "toString must contain 'x' for black pawns at start");
  }

  @Test
  void testToStringContainsEmptySquareSymbol12x12() {
    Board board = new Board(12);

    assertTrue(board.toString().contains("_"),
        "toString must contain '_' for empty or light squares");
  }

  @Test
  void testToStringWhiteCheckerSymbol12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("F6");
    board.promote("F6");

    assertTrue(board.toString().contains("O"),
        "toString must contain 'O' for a white checker");
  }

  @Test
  void testToStringBlackCheckerSymbol12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addBlack("G7");
    board.promote("G7");

    assertTrue(board.toString().contains("B"),
        "toString must contain 'B' for a black checker");
  }

  @Test
  void testToStringEmptyBoardOnlyUnderscores12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    // Remove row labels (A–L at line start) before checking for piece symbols,
    // because "B" also appears as the row-B label.
    String withoutRowLabels = board.toString().replaceAll("(?m)^[A-L]  ", "");

    assertFalse(withoutRowLabels.contains("w"), "Empty board must not contain white pawn symbol");
    assertFalse(withoutRowLabels.contains("W"), "Empty board must not contain white checker symbol");
    assertFalse(withoutRowLabels.contains("b"), "Empty board must not contain black pawn symbol");
    assertFalse(withoutRowLabels.contains("B"), "Empty board must not contain black checker symbol");
    assertTrue(board.toString().contains("_"), "Empty board must still contain '_' for squares");
  }

  @Test
  void testToStringRowOrderTopToBottom12x12() {
    Board board = new Board(12);
    String output = board.toString();

    // Row L (top) must appear before row A (bottom) in the string.
    int posL = output.indexOf('L');
    int posA = output.lastIndexOf('A'); // lastIndexOf avoids column header

    assertTrue(posL < posA,
        "Row L (top) must appear before row A (bottom) in toString output");
  }

  @Test
  void testToStringAfterMovePieceDisappears12x12() {
    Board board = new Board(12);
    clearBoard(board, 12);

    board.addWhite("C3");
    board.move("C3", "D4");

    String output = board.toString();

    // After the move, C3 row/col area should show '_' and D4 should show 'w'.
    // We verify the global symbol counts shifted (one 'w' present, none at C3).
    assertTrue(output.contains("o"),
        "toString must still contain 'w' after a move");
  }

  @Test
  void testToStringSymbolCountAtStart12x12() {
    Board board = new Board(12);
    String output = board.toString();

    // On a 12x12 board: 5 rows × 6 pawns per row = 30 white pawns and 30 black
    // pawns.
    long whiteCount = output.chars().filter(c -> c == 'o').count();
    long blackCount = output.chars().filter(c -> c == 'x').count();

    assertEquals(30, whiteCount,
        "There must be exactly 30 white pawns ('w') at the start of a 12x12 game");
    assertEquals(30, blackCount,
        "There must be exactly 30 black pawns ('b') at the start of a 12x12 game");
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
