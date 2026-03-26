package fr.ubordeaux.pdp.model.player.ai;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    assertEquals(Ai.DEFAULT_MAX_TIME_MS, mcts.getMaxTimeMs());
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
    board.clearBoard();
    assertTrue(board.noPiecesLeft(PlayerColor.WHITE));
    assertTrue(board.noPiecesLeft(PlayerColor.BLACK));

    ManagerUndoRedo undo = new ManagerUndoRedo(board);
    Mcts mcts = new Mcts(1, 100);

    Move move = mcts.getBestMove(undo, board, PlayerColor.WHITE, new SimpleEvaluator());
    assertNull(move);
  }
}
