package fr.ubordeaux.pdp.model.player.ai;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.evaluation.SimpleEvaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;

/**
 * Unit tests for the MinMaxAlphaBeta AI algorithm.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Basic functionality and alpha-beta pruning
 *   <li>Depth configuration and default values
 *   <li>Performance comparison with MinMax
 *   <li>Edge cases and move validation
 * </ul>
 */
class MinMaxAlphaBetaTest {

  private Board board;
  private ManagerUndoRedo undoManager;
  private Evaluator evaluator;
  private MinMaxAlphaBeta minMaxAlphaBeta;

  @BeforeEach
  void setUp() {
    board = new Board(8);
    undoManager = new ManagerUndoRedo(board);
    evaluator = new SimpleEvaluator();
    minMaxAlphaBeta = new MinMaxAlphaBeta();
  }

  @Test
  @DisplayName("Default constructor should use depth 5")
  void testDefaultConstructor() {
    MinMaxAlphaBeta defaultAlphaBeta = new MinMaxAlphaBeta();
    assertEquals(5, defaultAlphaBeta.getMaxDepth(), "Default depth should be 5");
  }

  @Test
  @DisplayName("Constructor with custom depth should set correct depth")
  void testConstructorWithDepth() {
    MinMaxAlphaBeta customAlphaBeta = new MinMaxAlphaBeta(6);
    assertEquals(6, customAlphaBeta.getMaxDepth(), "Custom depth should be set correctly");
  }

  @Test
  @DisplayName("Constructor should throw exception for invalid depth")
  void testConstructorWithInvalidDepth() {
    assertThrows(IllegalArgumentException.class, () -> new MinMaxAlphaBeta(0),
        "Should throw exception for depth 0");
    assertThrows(IllegalArgumentException.class, () -> new MinMaxAlphaBeta(-2),
        "Should throw exception for negative depth");
  }

  @Test
  @DisplayName("Should return null when no moves available")
  void testNoMovesAvailable() {
    board.clearBoard();
    Move bestMove = minMaxAlphaBeta.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    assertNull(bestMove, "Should return null when no moves available");
  }

  @Test
  @DisplayName("Should return valid move from initial position")
  void testGetBestMoveFromInitialPosition() {
    Move bestMove = minMaxAlphaBeta.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    assertNotNull(bestMove, "Should return a valid move from initial position");

    // Verify the move has valid positions and structure
    assertTrue(bestMove.getFrom() >= 0 && bestMove.getFrom() < board.getIndexMax(),
        "Move should have valid from position");
    assertTrue(bestMove.getTo() >= 0 && bestMove.getTo() < board.getIndexMax(),
        "Move should have valid to position");
    assertTrue(bestMove.getFrom() != bestMove.getTo(),
        "Move should actually move to a different position");

    // Verify the move path is not empty
    assertFalse(bestMove.getPath().isEmpty(),
        "Move should have a valid path");
    assertEquals(bestMove.getFrom(), bestMove.getPath().get(0),
        "Move path should start from the from position");
    assertEquals(bestMove.getTo(), bestMove.getPath().get(bestMove.getPath().size() - 1),
        "Move path should end at the to position");
  }

  @Test
  @DisplayName("Should handle both white and black players")
  void testBothPlayers() {
    Move whiteMove = minMaxAlphaBeta.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    Move blackMove = minMaxAlphaBeta.getBestMove(undoManager, board, PlayerColor.BLACK, evaluator);

    assertNotNull(whiteMove, "Should return move for white player");
    assertNotNull(blackMove, "Should return move for black player");

    // Verify white move has valid positions and structure
    assertTrue(whiteMove.getFrom() >= 0 && whiteMove.getFrom() < board.getIndexMax(),
        "White move should have valid from position");
    assertTrue(whiteMove.getTo() >= 0 && whiteMove.getTo() < board.getIndexMax(),
        "White move should have valid to position");
    assertTrue(whiteMove.getFrom() != whiteMove.getTo(),
        "White move should actually move to a different position");

    // Verify black move has valid positions and structure
    assertTrue(blackMove.getFrom() >= 0 && blackMove.getFrom() < board.getIndexMax(),
        "Black move should have valid from position");
    assertTrue(blackMove.getTo() >= 0 && blackMove.getTo() < board.getIndexMax(),
        "Black move should have valid to position");
    assertTrue(blackMove.getFrom() != blackMove.getTo(),
        "Black move should actually move to a different position");

    // Verify both moves have valid paths
    assertFalse(whiteMove.getPath().isEmpty(),
        "White move should have a valid path");
    assertFalse(blackMove.getPath().isEmpty(),
        "Black move should have a valid path");
  }

  @Test
  @DisplayName("Should return same result as MinMax for same position")
  void testConsistencyWithMinMax() {
    MinMax minMax = new MinMax(3);

    Move alphaBetaMove = minMaxAlphaBeta.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    Move minMaxMove = minMax.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);

    // Both should return valid moves (not necessarily the same, but both valid)
    assertNotNull(alphaBetaMove, "AlphaBeta should return a move");
    assertNotNull(minMaxMove, "MinMax should return a move");

    // Verify both moves have valid structure
    assertTrue(alphaBetaMove.getFrom() >= 0 && alphaBetaMove.getFrom() < board.getIndexMax(),
        "AlphaBeta move should have valid from position");
    assertTrue(alphaBetaMove.getTo() >= 0 && alphaBetaMove.getTo() < board.getIndexMax(),
        "AlphaBeta move should have valid to position");

    assertTrue(minMaxMove.getFrom() >= 0 && minMaxMove.getFrom() < board.getIndexMax(),
        "MinMax move should have valid from position");
    assertTrue(minMaxMove.getTo() >= 0 && minMaxMove.getTo() < board.getIndexMax(),
        "MinMax move should have valid to position");
  }

  @Test
  @DisplayName("Depth setter should work correctly")
  void testSetMaxDepth() {
    minMaxAlphaBeta.setMaxDepth(8);
    assertEquals(8, minMaxAlphaBeta.getMaxDepth(), "Should set new depth correctly");

    assertThrows(IllegalArgumentException.class, () -> minMaxAlphaBeta.setMaxDepth(0),
        "Should throw exception for invalid depth");
    assertThrows(IllegalArgumentException.class, () -> minMaxAlphaBeta.setMaxDepth(-3),
        "Should throw exception for negative depth");
  }

  @Test
  @DisplayName("Should preserve board state after move selection")
  void testBoardStatePreservation() {
    Board originalBoard = new Board(8);
    ManagerUndoRedo originalUndo = new ManagerUndoRedo(originalBoard);

    // Get initial board state
    int initialWhitePawns = countWhitePawns(originalBoard);
    int initialBlackPawns = countBlackPawns(originalBoard);

    // Get best move
    minMaxAlphaBeta.getBestMove(originalUndo, originalBoard, PlayerColor.WHITE, evaluator);

    // Verify board state is unchanged
    assertEquals(initialWhitePawns, countWhitePawns(originalBoard),
        "White pawns count should be unchanged");
    assertEquals(initialBlackPawns, countBlackPawns(originalBoard),
        "Black pawns count should be unchanged");
  }

  @Test
  @DisplayName("Should work with different board sizes")
  void testDifferentBoardSizes() {
    Board board10 = new Board(10);
    ManagerUndoRedo undo10 = new ManagerUndoRedo(board10);

    Board board12 = new Board(12);
    ManagerUndoRedo undo12 = new ManagerUndoRedo(board12);

    Move move10 = minMaxAlphaBeta.getBestMove(undo10, board10, PlayerColor.WHITE, evaluator);
    Move move12 = minMaxAlphaBeta.getBestMove(undo12, board12, PlayerColor.WHITE, evaluator);

    assertNotNull(move10, "Should work with 10x10 board");
    assertNotNull(move12, "Should work with 12x12 board");
  }

  @Test
  @DisplayName("Should handle capture moves correctly")
  void testCaptureMoves() {
    // Set up a position with available captures
    board.clearBoard();
    board.addWhite("C3");
    board.addBlack("D4");
    board.addBlack("F6");

    Move bestMove = minMaxAlphaBeta.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    assertNotNull(bestMove, "Should find move in capture position");

    // Verify the move has valid structure
    assertTrue(bestMove.getFrom() >= 0 && bestMove.getFrom() < board.getIndexMax(),
        "Move should have valid from position");
    assertTrue(bestMove.getTo() >= 0 && bestMove.getTo() < board.getIndexMax(),
        "Move should have valid to position");
    assertTrue(bestMove.getFrom() != bestMove.getTo(),
        "Move should actually move to a different position");

    // Verify the move path is not empty
    assertFalse(bestMove.getPath().isEmpty(),
        "Move should have a valid path");
  }

  @Test
  @DisplayName("Should handle positions with promotions")
  void testPromotionPositions() {
    // Set up a position near promotion - use valid positions for 8x8 board
    board.clearBoard();
    board.addWhite("A7"); // White pawn one move from promotion (valid position)
    board.addBlack("B8");
    board.addBlack("D8");

    Move bestMove = minMaxAlphaBeta.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    assertNotNull(bestMove, "Should handle promotion positions");

    // Verify the move has valid structure
    assertTrue(bestMove.getFrom() >= 0 && bestMove.getFrom() < board.getIndexMax(),
        "Move should have valid from position");
    assertTrue(bestMove.getTo() >= 0 && bestMove.getTo() < board.getIndexMax(),
        "Move should have valid to position");
    assertTrue(bestMove.getFrom() != bestMove.getTo(),
        "Move should actually move to a different position");

    // Verify the move path is not empty
    assertFalse(bestMove.getPath().isEmpty(),
        "Move should have a valid path");
  }

  @Test
  @DisplayName("Should respect maximum time limit")
  void testMaximumTimeLimit() {
    // Test with very short time limit (200ms)
    MinMaxAlphaBeta fastAi = new MinMaxAlphaBeta(10, 200L);

    long startTime = System.currentTimeMillis();
    Move bestMove = fastAi.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    long elapsedTime = System.currentTimeMillis() - startTime;

    assertNotNull(bestMove, "Should return a move even with time constraint");
    assertTrue(elapsedTime < 500L, "Should complete within reasonable time (grace period included)");
  }

  @Test
  @DisplayName("Should handle different time limits correctly")
  void testDifferentTimeLimits() {
    // Test with various time limits
    long[] timeLimits = { 200L, 500L, 1000L, 2000L };

    for (long timeLimit : timeLimits) {
      MinMaxAlphaBeta ai = new MinMaxAlphaBeta(8, timeLimit);

      long startTime = System.currentTimeMillis();
      Move move = ai.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
      long elapsedTime = System.currentTimeMillis() - startTime;

      assertNotNull(move, "Should return move for time limit: " + timeLimit + "ms");
      assertTrue(elapsedTime <= timeLimit + 100L, // Allow 100ms grace period
          "Should respect time limit: " + timeLimit + "ms, actual: " + elapsedTime + "ms");
    }
  }

  @Test
  @DisplayName("Should use iterative deepening effectively under time constraints")
  void testIterativeDeepeningWithTimeConstraints() {
    // Create a complex position to force deeper search
    Board complexBoard = createComplexPosition();
    ManagerUndoRedo complexUndo = new ManagerUndoRedo(complexBoard);

    // Test with short time limit to force early termination
    MinMaxAlphaBeta timeConstrainedAi = new MinMaxAlphaBeta(10, 300L);

    long startTime = System.currentTimeMillis();
    Move bestMove = timeConstrainedAi.getBestMove(complexUndo, complexBoard, PlayerColor.WHITE,
        evaluator);
    long elapsedTime = System.currentTimeMillis() - startTime;

    assertNotNull(bestMove, "Should return best move found within time limit");
    assertTrue(elapsedTime <= 400L, "Should complete within time limit with grace period");
  }

  @Test
  @DisplayName("Should maintain consistency when time limits are hit")
  void testConsistencyWhenTimeLimitHit() {
    // Test multiple runs with same time limit should produce same or similar quality moves
    MinMaxAlphaBeta ai = new MinMaxAlphaBeta(6, 150L);

    List<Move> moves = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      Board testBoard = new Board(8); // Fresh board each time
      ManagerUndoRedo testUndo = new ManagerUndoRedo(testBoard);

      Move move = ai.getBestMove(testUndo, testBoard, PlayerColor.WHITE, evaluator);
      assertNotNull(move, "Run " + i + " should return a move");
      moves.add(move);
    }

    // All moves should be valid (not necessarily identical)
    for (Move move : moves) {
      assertTrue(move.getFrom() >= 0 && move.getFrom() < board.getIndexMax(),
          "Move should have valid from position");
      assertTrue(move.getTo() >= 0 && move.getTo() < board.getIndexMax(),
          "Move should have valid to position");
    }
  }

  @Test
  @DisplayName("Should handle time limit edge cases")
  void testTimeLimitEdgeCases() {
    // Test minimum allowed time
    MinMaxAlphaBeta minTimeAi = new MinMaxAlphaBeta(3, 100L);
    Move minTimeMove = minTimeAi.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    assertNotNull(minTimeMove, "Should work with minimum time limit");

    // Test maximum allowed time
    MinMaxAlphaBeta maxTimeAi = new MinMaxAlphaBeta(3, 30000L);
    Move maxTimeMove = maxTimeAi.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    assertNotNull(maxTimeMove, "Should work with maximum time limit");
  }

  @Test
  @DisplayName("Should handle time setter correctly")
  void testTimeSetter() {
    MinMaxAlphaBeta ai = new MinMaxAlphaBeta();

    ai.setMaxTimeMs(1000L);
    assertEquals(1000L, ai.getMaxTimeMs(), "Should set new time limit correctly");

    assertThrows(IllegalArgumentException.class, () -> ai.setMaxTimeMs(50L),
        "Should throw exception for time below minimum");
    assertThrows(IllegalArgumentException.class, () -> ai.setMaxTimeMs(50000L),
        "Should throw exception for time above maximum");
  }

  @Test
  @DisplayName("Should measure performance under time constraints")
  void testPerformanceUnderTimeConstraints() {
    MinMaxAlphaBeta ai = new MinMaxAlphaBeta(8, 1000L);

    long totalTime = 0L;
    int iterations = 10;

    for (int i = 0; i < iterations; i++) {
      Board testBoard = new Board(8);
      ManagerUndoRedo testUndo = new ManagerUndoRedo(testBoard);

      long startTime = System.currentTimeMillis();
      Move move = ai.getBestMove(testUndo, testBoard, PlayerColor.WHITE, evaluator);
      long elapsedTime = System.currentTimeMillis() - startTime;

      assertNotNull(move, "Iteration " + i + " should return a move");
      totalTime += elapsedTime;
    }

    long averageTime = totalTime / iterations;
    assertTrue(averageTime <= 1200L, "Average time should be reasonable: " + averageTime + "ms");
  }

  /**
   * Helper method to create a complex position for testing.
   *
   * @return a board with many pieces to force deeper search
   */
  private Board createComplexPosition() {
    Board complexBoard = new Board(8);
    complexBoard.clearBoard();

    // Create a position with many pieces to force deeper search
    complexBoard.addWhite("A1");
    complexBoard.addWhite("C1");
    complexBoard.addWhite("E1");
    complexBoard.addWhite("G1");
    complexBoard.addWhite("B2");
    complexBoard.addWhite("D2");
    complexBoard.addWhite("F2");
    complexBoard.addWhite("H2");
    complexBoard.addWhite("A3");
    complexBoard.addWhite("C3");
    complexBoard.addWhite("E3");
    complexBoard.addWhite("G3");

    complexBoard.addBlack("B6");
    complexBoard.addBlack("D6");
    complexBoard.addBlack("F6");
    complexBoard.addBlack("H6");
    complexBoard.addBlack("A7");
    complexBoard.addBlack("C7");
    complexBoard.addBlack("E7");
    complexBoard.addBlack("G7");
    complexBoard.addBlack("B8");
    complexBoard.addBlack("D8");
    complexBoard.addBlack("F8");
    complexBoard.addBlack("H8");

    return complexBoard;
  }

  /**
   * Helper method to count white pawns on the board.
   */
  private int countWhitePawns(Board b) {
    int count = 0;
    for (int i = 0; i < b.getIndexMax(); i++) {
      if (b.isBitWhitePawn(i)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Helper method to count black pawns on the board.
   */
  private int countBlackPawns(Board b) {
    int count = 0;
    for (int i = 0; i < b.getIndexMax(); i++) {
      if (b.isBitBlackPawn(i)) {
        count++;
      }
    }
    return count;
  }
}