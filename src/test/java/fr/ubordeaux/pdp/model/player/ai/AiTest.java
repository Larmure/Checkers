package fr.ubordeaux.pdp.model.player.ai;

import static org.junit.jupiter.api.Assertions.*;
import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import fr.ubordeaux.pdp.model.tools.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiTest {

  private ManagerUndoRedo undo;
  private Board board;
  private Evaluator evaluator;

  private Ai ai;

  @BeforeEach
  void setUp() {
    undo = Mockito.mock(ManagerUndoRedo.class);
    board = Mockito.mock(Board.class);
    evaluator = Mockito.mock(Evaluator.class);

    ai = new MinMax(3, 1000);
  }

  // =============================
  // TEST validateParameters
  // =============================

  @Test
  void testGetBestMove_nullUndo() {
    assertThrows(IllegalArgumentException.class, () -> ai.getBestMove(null, board, PlayerColor.WHITE, evaluator));
  }

  @Test
  void testGetBestMove_nullBoard() {
    assertThrows(IllegalArgumentException.class, () -> ai.getBestMove(undo, null, PlayerColor.WHITE, evaluator));
  }

  @Test
  void testGetBestMove_nullPlayer() {
    assertThrows(IllegalArgumentException.class, () -> ai.getBestMove(undo, board, null, evaluator));
  }

  @Test
  void testGetBestMove_nullEvaluator() {
    assertThrows(IllegalArgumentException.class, () -> ai.getBestMove(undo, board, PlayerColor.WHITE, null));
  }

  @Test
  void testGetBestMove_noMoves_returnsNull() {
    Mockito.when(board.getWhiteValidMoves()).thenReturn(java.util.List.of());

    Move result = ai.getBestMove(undo, board, PlayerColor.WHITE, evaluator);

    assertNull(result);
  }

  // =============================
  // TEST getRemainingTime
  // =============================

  @Test
  void testGetRemainingTime_notExceeded() {
    long startTime = System.currentTimeMillis();

    long remaining = ai.getRemainingTime(startTime);

    assertTrue(remaining > 0);
    assertTrue(remaining <= ai.getMaxTimeMs());
  }

  @Test
  void testGetRemainingTime_exceeded() throws InterruptedException {
    Ai smallTimeAi = new MinMax(3, 100); // 100ms

    long startTime = System.currentTimeMillis();
    Thread.sleep(150);

    long remaining = smallTimeAi.getRemainingTime(startTime);

    assertEquals(0, remaining);
  }

  // =============================
  // TEST buildAi
  // =============================

  @Test
  void testBuildAi_minimax() {
    Ai ai = Ai.buildAi(buildAiConfiguration("minimax"));

    assertNotNull(ai);
    assertTrue(ai instanceof MinMax);
  }

  @Test
  void testBuildAi_alphabeta() {
    Ai ai = Ai.buildAi(buildAiConfiguration("alphabeta"));

    assertNotNull(ai);
    assertTrue(ai instanceof MinMaxAlphaBeta);
  }

  @Test
  void testBuildAi_mcts() {
    Ai ai = Ai.buildAi(buildAiConfiguration("mcts"));

    assertNotNull(ai);
    assertTrue(ai instanceof Mcts);
  }

  @Test
  void testBuildAi_invalidMode() {
    Ai ai = Ai.buildAi(buildAiConfiguration("invalid"));

    assertNotNull(ai);
    assertTrue(ai instanceof MinMax);
  }

  // @Test
  // void testBuildAi_iterative_returnsNull() {
  //   Ai ai = Ai.buildAi(buildAiConfiguration("iterative"));

  //   assertNull(ai);
  // }

  private Configuration buildAiConfiguration(String aiMode) {
    return new Configuration(Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME,
        Utils.DEFAULT_CONTEST, Utils.DEFAULT_BOARD_SIZE, Utils.DEFAULT_VERBOSE, Utils.DEFAULT_DEBUG,
        Utils.DEFAULT_WHITE_AI, Utils.DEFAULT_BLACK_AI, Ai.DEFAULT_MAX_TIME_MS, aiMode,
        Ai.DEFAULT_DEPTH, Mcts.DEFAULT_SELECTION_MODE);
  }
}