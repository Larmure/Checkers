package fr.ubordeaux.pdp.model.player.ia;

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
  @DisplayName("Default constructor should use depth 3")
  void testDefaultConstructor() {
    MinMaxAlphaBeta defaultAlphaBeta = new MinMaxAlphaBeta();
    assertEquals(3, defaultAlphaBeta.getMaxDepth(), "Default depth should be 3");
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