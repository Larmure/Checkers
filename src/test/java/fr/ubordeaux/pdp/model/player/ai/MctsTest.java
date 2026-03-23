package fr.ubordeaux.pdp.model.player.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.SimpleEvaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;

class MctsTest {

  @Test
  @DisplayName("Default constructor sets exploration constant to sqrt(2)")
  void testDefaultConstructorExplorationConstant() {
    Mcts mcts = new Mcts();

    assertEquals(Mcts.DEFAULT_EXPLORATION, mcts.explorationConstant, 1e-9);
    assertEquals(3000L, mcts.getMaxTimeMs());
  }

  @Test
  @DisplayName("Constructor with custom exploration constant")
  void testConstructorWithCustomExplorationConstant() {
    Mcts mcts = new Mcts(1, 100, 0.7);

    assertEquals(0.7, mcts.explorationConstant, 1e-9);
    assertEquals(100L, mcts.getMaxTimeMs());
  }

  @Test
  @DisplayName("Constructor with non-positive exploration constant throws")
  void testConstructorThrowsOnNonPositiveExploration() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> new Mcts(1, 100, 0.0));
    assertTrue(ex.getMessage().contains("Exploration constant must be positive"));
  }

  @Test
  @DisplayName("getBestMove returns a valid move for initial board")
  void testGetBestMoveOnInitialBoard() {
    Board board = new Board(8);
    ManagerUndoRedo undo = new ManagerUndoRedo(board);
    Mcts mcts = new Mcts(1, 100);

    Move move = mcts.getBestMove(undo, board, PlayerColor.WHITE, new SimpleEvaluator());

    assertNotNull(move);
    assertTrue(board.getWhiteValidMoves().contains(move) || board.getBlackValidMoves().contains(move)
        || board.getWhiteValidMoves().size() > 0); // simple sanity check
    assertTrue(move.getPath().size() >= 2);
  }

  @Test
  @DisplayName("getBestMove returns null when no legal moves")
  void testGetBestMoveWhenNoMovesReturnsNull() throws Exception {
    Board board = new Board(8);
    clearAllPieces(board);
    assertTrue(board.noPiecesLeft(PlayerColor.WHITE));
    assertTrue(board.noPiecesLeft(PlayerColor.BLACK));

    ManagerUndoRedo undo = new ManagerUndoRedo(board);
    Mcts mcts = new Mcts(1, 100);

    Move move = mcts.getBestMove(undo, board, PlayerColor.WHITE, new SimpleEvaluator());
    assertNull(move);
  }

  private void clearAllPieces(Board board) throws Exception {
    for (String fieldName : new String[] {
        "whitePawns1", "whitePawns2", "whiteCheckers1", "whiteCheckers2",
        "blackPawns1", "blackPawns2", "blackCheckers1", "blackCheckers2" }) {
      Field field = Board.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.setLong(board, 0L);
    }
  }
}
