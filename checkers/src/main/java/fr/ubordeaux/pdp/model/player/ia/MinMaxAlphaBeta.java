package fr.ubordeaux.pdp.model.player.ia;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.util.List;

/**
 * Implementation of the MinMax algorithm with Alpha-Beta pruning for checkers AI.
 *
 * <p>This algorithm extends the classic MinMax approach with alpha-beta pruning,
 * which significantly reduces the number of nodes evaluated in the game tree.
 * The alpha-beta pruning works by maintaining two values:
 * <ul>
 *   <li>alpha: the best value that the MAX player can guarantee so far
 *   <li>beta: the best value that the MIN player can guarantee so far
 * </ul>
 *
 * <p>When beta becomes less than or equal to alpha, the algorithm can prune
 * the remaining branches as they won't affect the final decision.
 *
 * <p>The algorithm supports:
 * <ul>
 *   <li>Configurable search depth (default: 3)
 *   <li>Separate MaxValue and MinValue functions for clarity
 *   <li>Alpha-beta pruning for performance optimization
 *   <li>Undo/Redo support for move management
 *   <li>Integration with any Evaluator implementation
 * </ul>
 */
public class MinMaxAlphaBeta implements AI {

  /** Default search depth when not specified. */
  private static final int DEFAULT_DEPTH = 3;

  /** Maximum search depth for the algorithm. */
  private int maxDepth;

  /**
   * Creates a MinMaxAlphaBeta AI with the default search depth of 3.
   */
  public MinMaxAlphaBeta() {
    this.maxDepth = DEFAULT_DEPTH;
  }

  /**
   * Creates a MinMaxAlphaBeta AI with a specified search depth.
   *
   * @param depth the maximum search depth (must be positive)
   */
  public MinMaxAlphaBeta(int depth) {
    if (depth <= 0) {
      throw new IllegalArgumentException("Depth must be positive, got: " + depth);
    }
    this.maxDepth = depth;
  }

  /**
   * Returns the best move for the current player using the MinMax algorithm with Alpha-Beta pruning.
   *
   * <p>This method evaluates all possible moves for the current player and selects the one
   * with the best evaluation score according to the alpha-beta algorithm. The pruning
   * significantly reduces the search space compared to pure MinMax.
   *
   * @param undo the undo/redo manager for move operations
   * @param board the current game board state
   * @param player the current player color
   * @param evaluator the evaluation function to score board positions
   * @return the best move according to Alpha-Beta MinMax, or null if no moves are available
   */
  @Override
  public Move getBestMove(ManagerUndoRedo undo, Board board, PlayerColor player, Evaluator evaluator) {
    List<Move> moves = player == PlayerColor.WHITE ? board.getWhiteValidMoves() : board.getBlackValidMoves();

    if (moves.isEmpty()) {
      return null;
    }

    Move bestMove = moves.get(0);
    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;

    if (player == PlayerColor.WHITE) {
      int bestValue = Integer.MIN_VALUE;
      for (Move move : moves) {
        undo.registerMove(player, move);
        board.applyMove(move);

        int value = minValue(undo, board, evaluator, maxDepth - 1, alpha, beta);

        undo.undo(true);

        if (value > bestValue) {
          bestValue = value;
          bestMove = move;
        }
        alpha = Math.max(alpha, bestValue);
      }
    } else {
      int bestValue = Integer.MAX_VALUE;
      for (Move move : moves) {
        undo.registerMove(player, move);
        board.applyMove(move);

        int value = maxValue(undo, board, evaluator, maxDepth - 1, alpha, beta);

        undo.undo(false);

        if (value < bestValue) {
          bestValue = value;
          bestMove = move;
        }
        beta = Math.min(beta, bestValue);
      }
    }

    return bestMove;
  }

  /**
   * MaxValue function of the Alpha-Beta MinMax algorithm.
   *
   * <p>This function represents the WHITE player's turn and tries to maximize the evaluation
   * score. It uses alpha-beta pruning to cut off branches that won't affect the final result.
   * When the current best value (alpha) is greater than or equal to beta, the function
   * returns immediately without exploring further branches.
   *
   * @param undo the undo/redo manager for move operations
   * @param board the current game board state
   * @param evaluator the evaluation function to score board positions
   * @param depth the remaining search depth
   * @param alpha the best value that the MAX player can guarantee so far
   * @param beta the best value that the MIN player can guarantee so far
   * @return the maximum evaluation score achievable from this position
   */
  private int maxValue(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth, int alpha, int beta) {
    if (depth == 0 || board.noPiecesLeft(PlayerColor.WHITE) || board.noPiecesLeft(PlayerColor.BLACK)) {
      return evaluator.evaluate(board);
    }

    List<Move> moves = board.getWhiteValidMoves();
    int maxEval = Integer.MIN_VALUE;

    for (Move move : moves) {
      undo.registerMove(PlayerColor.WHITE, move);
      board.applyMove(move);

      int evalValue = minValue(undo, board, evaluator, depth - 1, alpha, beta);

      undo.undo(true);

      maxEval = Math.max(maxEval, evalValue);
      alpha = Math.max(alpha, evalValue);

      // Alpha-beta pruning: if alpha >= beta, we can prune remaining branches
      if (beta <= alpha) {
        break;
      }
    }

    return maxEval;
  }

  /**
   * MinValue function of the Alpha-Beta MinMax algorithm.
   *
   * <p>This function represents the BLACK player's turn and tries to minimize the evaluation
   * score. It uses alpha-beta pruning to cut off branches that won't affect the final result.
   * When beta becomes less than or equal to alpha, the function returns immediately
   * without exploring further branches.
   *
   * @param undo the undo/redo manager for move operations
   * @param board the current game board state
   * @param evaluator the evaluation function to score board positions
   * @param depth the remaining search depth
   * @param alpha the best value that the MAX player can guarantee so far
   * @param beta the best value that the MIN player can guarantee so far
   * @return the minimum evaluation score achievable from this position
   */
  private int minValue(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth, int alpha, int beta) {
    if (depth == 0 || board.noPiecesLeft(PlayerColor.WHITE) || board.noPiecesLeft(PlayerColor.BLACK)) {
      return evaluator.evaluate(board);
    }

    List<Move> moves = board.getBlackValidMoves();
    int minEval = Integer.MAX_VALUE;

    for (Move move : moves) {
      undo.registerMove(PlayerColor.BLACK, move);
      board.applyMove(move);

      int evalValue = maxValue(undo, board, evaluator, depth - 1, alpha, beta);

      undo.undo(false);

      minEval = Math.min(minEval, evalValue);
      beta = Math.min(beta, evalValue);

      // Alpha-beta pruning: if beta <= alpha, we can prune remaining branches
      if (beta <= alpha) {
        break;
      }
    }

    return minEval;
  }

  /**
   * Returns the current maximum search depth.
   *
   * @return the maximum search depth used by this algorithm
   */
  public int getMaxDepth() {
    return maxDepth;
  }

  /**
   * Sets the maximum search depth.
   *
   * @param depth the new maximum search depth (must be positive)
   * @throws IllegalArgumentException if depth is not positive
   */
  public void setMaxDepth(int depth) {
    if (depth <= 0) {
      throw new IllegalArgumentException("Depth must be positive, got: " + depth);
    }
    this.maxDepth = depth;
  }
}