package fr.ubordeaux.pdp.model.player.ai;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.util.List;

/**
 * Iterative deepening wrapper around the {@link MinMax} algorithm.
 *
 * <p>Explores the game tree depth by depth, from 1 up to {@code maxDepth}. After each
 * fully completed depth, the best move found is retained. If the time limit expires
 * during a depth search, the best move from the last fully completed depth is returned
 * rather than a partial, potentially unreliable result.
 *
 * <p>This approach guarantees that a valid move is always available even under strict
 * time constraints, while progressively improving the quality of the result as time
 * allows.
 */
public class IterativeDeepening extends Ai {

  /** The underlying MinMax instance used to perform the search at each depth level. */
  private final MinMax minMax;

  /** Creates an IterativeDeepening AI with default depth and time settings. */
  public IterativeDeepening() {
    super();
    this.minMax = new MinMax(maxDepth, maxTimeMs);
  }

  /**
   * Creates an IterativeDeepening AI with the specified search depth and default time limit.
   *
   * @param depth the maximum search depth
   */
  public IterativeDeepening(int depth) {
    super(depth);
    this.minMax = new MinMax(depth, maxTimeMs);
  }

  /**
   * Creates an IterativeDeepening AI with the specified search depth and time limit.
   *
   * @param depth     the maximum search depth
   * @param maxTimeMs the maximum thinking time in milliseconds
   */
  public IterativeDeepening(int depth, long maxTimeMs) {
    super(depth, maxTimeMs);
    this.minMax = new MinMax(depth, maxTimeMs);
  }

  /**
   * Calculates the best move using iterative deepening over successive MinMax searches.
   *
   * <p>Runs a full single-pass MinMax search at depth 1, then depth 2, and so on up to
   * {@code maxDepth}. After each fully completed depth, its result replaces the current
   * best move. If the time limit is exceeded before a depth completes, the search is
   * aborted and the best move from the last fully completed depth is returned.
   *
   * <p>The first valid move is used as an initial fallback in case the time limit expires
   * before even the first depth completes.
   *
   * @param undo       the undo manager used to apply and revert moves
   * @param board      the current state of the game board
   * @param player     the color of the current player
   * @param evaluator  the evaluation function used to score board positions
   * @param validMoves the list of valid moves available to the current player
   * @return the best move found within the time and depth constraints
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
      minMax.setMaxDepth(currentDepth);
      Move candidate = minMax.calculateBestMove(undo, board, player, evaluator, validMoves);
      if (candidate != null) {
        bestMove = candidate;
      }
    }
    return bestMove;
  }
}