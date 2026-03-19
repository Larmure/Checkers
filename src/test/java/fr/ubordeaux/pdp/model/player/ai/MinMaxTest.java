package fr.ubordeaux.pdp.model.player.ai;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.evaluation.SimpleEvaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;

/**
 * Unit tests for the MinMax AI algorithm.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Basic functionality and move selection
 *   <li>Depth configuration and default values
 *   <li>Edge cases (empty moves, terminal positions)
 *   <li>Move validation and undo/redo operations
 * </ul>
 */
class MinMaxTest {

  private Board board;
  private ManagerUndoRedo undoManager;
  private Evaluator evaluator;
  private MinMax minMax;

  @BeforeEach
  void setUp() {
    board = new Board(8);
    undoManager = new ManagerUndoRedo(board);
    evaluator = new SimpleEvaluator();
    minMax = new MinMax();
  }

  @Test
  @DisplayName("Default constructor should use depth 3")
  void testDefaultConstructor() {
    MinMax defaultMinMax = new MinMax();
    assertEquals(5, defaultMinMax.getMaxDepth(), "Default depth should be 5");
  }

  @Test
  @DisplayName("Constructor with custom depth should set correct depth")
  void testConstructorWithDepth() {
    MinMax customMinMax = new MinMax(5);
    assertEquals(5, customMinMax.getMaxDepth(), "Custom depth should be set correctly");
  }

  @Test
  @DisplayName("Constructor should throw exception for invalid depth")
  void testConstructorWithInvalidDepth() {
    assertThrows(IllegalArgumentException.class, () -> new MinMax(0),
        "Should throw exception for depth 0");
    assertThrows(IllegalArgumentException.class, () -> new MinMax(-1),
        "Should throw exception for negative depth");
  }

  @Test
  @DisplayName("Should return null when no moves available")
  void testNoMovesAvailable() {
    board.clearBoard();
    Move bestMove = minMax.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    assertNull(bestMove, "Should return null when no moves available");
  }

  @Test
  @DisplayName("Should return valid move from initial position")
  void testGetBestMoveFromInitialPosition() {
    Move bestMove = minMax.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
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
    Move whiteMove = minMax.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    Move blackMove = minMax.getBestMove(undoManager, board, PlayerColor.BLACK, evaluator);

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
  @DisplayName("Should handle terminal positions correctly")
  void testTerminalPosition() {
    board.clearBoard();
    // Add only white pieces (black has no pieces - terminal position)
    board.addWhite("A1");
    board.addWhite("C1");

    Move bestMove = minMax.getBestMove(undoManager, board, PlayerColor.WHITE, evaluator);
    // Should still return a move if available, even in terminal positions
    if (!board.getWhiteValidMoves().isEmpty()) {
      assertNotNull(bestMove, "Should handle terminal positions gracefully");
    }
  }

  @Test
  @DisplayName("Depth setter should work correctly")
  void testSetMaxDepth() {
    minMax.setMaxDepth(7);
    assertEquals(7, minMax.getMaxDepth(), "Should set new depth correctly");

    assertThrows(IllegalArgumentException.class, () -> minMax.setMaxDepth(0),
        "Should throw exception for invalid depth");
    assertThrows(IllegalArgumentException.class, () -> minMax.setMaxDepth(-5),
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
    minMax.getBestMove(originalUndo, originalBoard, PlayerColor.WHITE, evaluator);

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

    Move move10 = minMax.getBestMove(undo10, board10, PlayerColor.WHITE, evaluator);
    Move move12 = minMax.getBestMove(undo12, board12, PlayerColor.WHITE, evaluator);

    assertNotNull(move10, "Should work with 10x10 board");
    assertNotNull(move12, "Should work with 12x12 board");
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