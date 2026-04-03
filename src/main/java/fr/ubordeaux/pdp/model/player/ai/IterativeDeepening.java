package fr.ubordeaux.pdp.model.player.ai;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.util.List;

/**
 * Iterative deepening wrapper around the MinMax algorithm.
 *
 * <p>Explores the game tree depth by depth, from 1 up to {@code maxDepth}.
 * After each fully completed depth, the best move found is retained.
 * If the time limit expires during a depth search, the best move from
 * the last fully completed depth is returned.
 */
public class IterativeDeepening extends Ai {

  private final MinMax minMax;

  public IterativeDeepening() {
    super();
    this.minMax = new MinMax(maxDepth, maxTimeMs);
  }

  public IterativeDeepening(int depth) {
    super(depth);
    this.minMax = new MinMax(depth, maxTimeMs);
  }

  public IterativeDeepening(int depth, long maxTimeMs) {
    super(depth, maxTimeMs);
    this.minMax = new MinMax(depth, maxTimeMs);
  }

  /**
   * Calculates the best move using iterative deepening over MinMax searches.
   *
   * <p>Runs a full MinMax search at depth 1, then 2, etc. Each depth is a
   * complete single-pass MinMax. If a depth completes within the time limit,
   * its result is saved. If time expires mid-depth, the previous result is kept.
   */
  @Override
  protected Move calculateBestMove(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> validMoves) {
    long startTime = System.currentTimeMillis();
    Move bestMove = validMoves.get(0);

    for (int currentDepth = 1; currentDepth <= maxDepth; currentDepth++) {
      if (isTimeExceeded(startTime)) {
        break;
      }
      minMax.setMaxDepth(currentDepth); // expose a setter in MinMax
      Move candidate = minMax.calculateBestMove(undo, board, player, evaluator, validMoves);
      if (candidate != null) {
        bestMove = candidate;
      }
    }
    return bestMove;
  }
}