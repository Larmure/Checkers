package fr.ubordeaux.pdp.model.player.ia;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.util.List;

/**
 * Implementation of the MinMax algorithm for checkers AI.
 *
 * <p>This algorithm uses the classic MinMax approach with separate functions for MAX and MIN
 * players. The MAX player (WHITE) tries to maximize the evaluation score, while the MIN player
 * (BLACK) tries to minimize it. The algorithm explores the game tree up to a specified depth
 * and uses an evaluation function to estimate the value of leaf nodes.
 *
 * <p>The algorithm supports:
 * <ul>
 *   <li>Configurable search depth (default: 3)
 *   <li>Separate MAX and MIN functions for clarity
 *   <li>Undo/Redo support for move management
 *   <li>Integration with any Evaluator implementation
 * </ul>
 */
public class MinMax implements AI {

  /** Default search depth when not specified. */
  private static final int DEFAULT_DEPTH = 3;

  /** Maximum search depth for the algorithm. */
  private int maxDepth;

  /**
   * Creates a MinMax AI with the default search depth of 3.
   */
  public MinMax() {
    this.maxDepth = DEFAULT_DEPTH;
  }

  /**
   * Creates a MinMax AI with a specified search depth.
   *
   * @param depth the maximum search depth (must be positive)
   */
  public MinMax(int depth) {
    if (depth <= 0) {
      throw new IllegalArgumentException("Depth must be positive, got: " + depth);
    }
    this.maxDepth = depth;
  }

  /**
   * Returns the best move for the current player using the MinMax algorithm.
   *
   * <p>This method evaluates all possible moves for the current player and selects the one
   * with the best evaluation score according to the MinMax algorithm. For WHITE players,
   * it selects the move with the maximum score, while for BLACK players, it selects the
   * move with the minimum score.
   *
   * @param undo the undo/redo manager for move operations
   * @param board the current game board state
   * @param player the current player color
   * @param evaluator the evaluation function to score board positions
   * @return the best move according to MinMax, or null if no moves are available
   */
  @Override
  public Move getBestMove(ManagerUndoRedo undo, Board board, PlayerColor player, Evaluator evaluator) {
    List<Move> moves = player == PlayerColor.WHITE ? board.getWhiteValidMoves() : board.getBlackValidMoves();

    if (moves.isEmpty()) {
      return null;
    }

    Move bestMove = moves.get(0);
    int bestValue;

    if (player == PlayerColor.WHITE) {
      bestValue = Integer.MIN_VALUE;
      for (Move move : moves) {
        undo.registerMove(player, move);
        board.applyMove(move);

        int value = minMax(undo, board, evaluator, maxDepth - 1);

        undo.undo(true);

        if (value > bestValue) {
          bestValue = value;
          bestMove = move;
        }
      }
    } else {
      bestValue = Integer.MAX_VALUE;
      for (Move move : moves) {
        undo.registerMove(player, move);
        board.applyMove(move);

        int value = maxMin(undo, board, evaluator, maxDepth - 1);

        undo.undo(false);

        if (value < bestValue) {
          bestValue = value;
          bestMove = move;
        }
      }
    }

    return bestMove;
  }

  /**
   * MAX function of the MinMax algorithm.
   *
   * <p>This function represents the WHITE player's turn and tries to maximize the evaluation
   * score. It recursively calls the MIN function for the opponent's turn.
   *
   * @param undo the undo/redo manager for move operations
   * @param board the current game board state
   * @param evaluator the evaluation function to score board positions
   * @param depth the remaining search depth
   * @return the maximum evaluation score achievable from this position
   */
  private int maxMin(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth) {
    if (depth == 0 || board.noPiecesLeft(PlayerColor.WHITE) || board.noPiecesLeft(PlayerColor.BLACK)) {
      return evaluator.evaluate(board);
    }

    List<Move> moves = board.getWhiteValidMoves();
    int maxEval = Integer.MIN_VALUE;

    for (Move move : moves) {
      undo.registerMove(PlayerColor.WHITE, move);
      board.applyMove(move);

      int evalValue = minMax(undo, board, evaluator, depth - 1);

      undo.undo(true);

      maxEval = Math.max(maxEval, evalValue);
    }

    return maxEval;
  }

  /**
   * MIN function of the MinMax algorithm.
   *
   * <p>This function represents the BLACK player's turn and tries to minimize the evaluation
   * score. It recursively calls the MAX function for the opponent's turn.
   *
   * @param undo the undo/redo manager for move operations
   * @param board the current game board state
   * @param evaluator the evaluation function to score board positions
   * @param depth the remaining search depth
   * @return the minimum evaluation score achievable from this position
   */
  private int minMax(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth) {
    if (depth == 0 || board.noPiecesLeft(PlayerColor.WHITE) || board.noPiecesLeft(PlayerColor.BLACK)) {
      return evaluator.evaluate(board);
    }

    List<Move> moves = board.getBlackValidMoves();
    int minEval = Integer.MAX_VALUE;

    for (Move move : moves) {
      undo.registerMove(PlayerColor.BLACK, move);
      board.applyMove(move);

      int evalValue = maxMin(undo, board, evaluator, depth - 1);

      undo.undo(false);

      minEval = Math.min(minEval, evalValue);
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