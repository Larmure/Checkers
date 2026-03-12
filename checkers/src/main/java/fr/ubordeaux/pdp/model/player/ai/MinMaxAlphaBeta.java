package fr.ubordeaux.pdp.model.player.ai;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.util.List;

/**
 * Implementation of the MinMax algorithm with Alpha-Beta pruning for checkers
 * AI.
 *
 * <p>
 * This algorithm extends the classic MinMax approach with alpha-beta pruning,
 * which significantly reduces the number of nodes evaluated in the game tree.
 * The alpha-beta pruning works by maintaining two values:
 * <ul>
 * <li>alpha: the best value that the MAX player can guarantee so far
 * <li>beta: the best value that the MIN player can guarantee so far
 * </ul>
 *
 * <p>
 * When beta becomes less than or equal to alpha, the algorithm can prune
 * the remaining branches as they won't affect the final decision.
 *
 * <p>
 * The algorithm supports:
 * <ul>
 * <li>Configurable search depth (default: 3)
 * <li>Separate MaxValue and MinValue functions for clarity
 * <li>Alpha-beta pruning for performance optimization
 * <li>Undo/Redo support for move management
 * <li>Integration with any Evaluator implementation
 * <li>Time-limited search with iterative deepening
 * </ul>
 */
public class MinMaxAlphaBeta extends AI {

  /**
   * Creates a MinMaxAlphaBeta AI with default depth and time settings.
   */
  public MinMaxAlphaBeta() {
    super();
  }

  /**
   * Creates a MinMaxAlphaBeta AI with specified depth and default time.
   *
   * @param depth the maximum search depth
   */
  public MinMaxAlphaBeta(int depth) {
    super(depth);
  }

  /**
   * Creates a MinMaxAlphaBeta AI with specified depth and time.
   *
   * @param depth     the maximum search depth
   * @param maxTimeMs the maximum thinking time in milliseconds
   */
  public MinMaxAlphaBeta(int depth, long maxTimeMs) {
    super(depth, maxTimeMs);
  }

  /**
   * Template method implementation for calculating the best move using Alpha-Beta
   * pruning.
   *
   * <p>
   * This method uses iterative deepening with time constraints to find the best
   * move
   * within the allotted time. It progressively increases the search depth until
   * either
   * the maximum depth is reached or the time limit is exceeded.
   *
   * @param undo       the undo/redo manager for move operations
   * @param board      the current game board state
   * @param player     the current player color
   * @param evaluator  the evaluation function to score board positions
   * @param validMoves list of valid moves for the current player
   * @return the best move according to Alpha-Beta MinMax
   */
  @Override
  protected Move calculateBestMove(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> validMoves) {

    long startTime = System.currentTimeMillis();
    Move bestMove = validMoves.get(0);
    int currentDepth = 1;

    // Recherche itérative avec limite de temps et alpha-beta pruning
    while (currentDepth <= maxDepth && !isTimeExceeded(startTime)) {
      Move tempBestMove = findBestMoveAtDepth(undo, board, player, evaluator, validMoves, currentDepth, startTime);

      if (tempBestMove != null) {
        bestMove = tempBestMove;
      }
      currentDepth++;
    }

    return bestMove;
  }

  /**
   * Finds the best move at a specific depth using Alpha-Beta pruning.
   *
   * @param undo      the undo/redo manager for move operations
   * @param board     the current game board state
   * @param player    the current player color
   * @param evaluator the evaluation function to score board positions
   * @param moves     list of valid moves to consider
   * @param depth     the current search depth
   * @param startTime the start time for time management
   * @return the best move at this depth
   */
  private Move findBestMoveAtDepth(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> moves, int depth, long startTime) {

    Move bestMove = null;
    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;

    if (player == PlayerColor.WHITE) {
      int bestValue = Integer.MIN_VALUE;
      for (Move move : moves) {
        if (isTimeExceeded(startTime))
          break;

        undo.registerMove(player, move);
        board.applyMove(move);

        int value = minValue(undo, board, evaluator, depth - 1, alpha, beta, startTime);

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
        if (isTimeExceeded(startTime))
          break;

        undo.registerMove(player, move);
        board.applyMove(move);

        int value = maxValue(undo, board, evaluator, depth - 1, alpha, beta, startTime);

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
   * MaxValue function of the Alpha-Beta MinMax algorithm with time management.
   *
   * <p>
   * This function represents the WHITE player's turn and tries to maximize the
   * evaluation score. It uses alpha-beta pruning to cut off branches that won't
   * affect
   * the final result and respects time constraints.
   *
   * @param undo      the undo/redo manager for move operations
   * @param board     the current game board state
   * @param evaluator the evaluation function to score board positions
   * @param depth     the remaining search depth
   * @param alpha     the best value that the MAX player can guarantee so far
   * @param beta      the best value that the MIN player can guarantee so far
   * @param startTime the start time for time management
   * @return the maximum evaluation score achievable from this position
   */
  private int maxValue(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth,
      int alpha, int beta, long startTime) {
    if (depth == 0 || board.noPiecesLeft(PlayerColor.WHITE) || board.noPiecesLeft(PlayerColor.BLACK)
        || isTimeExceeded(startTime)) {
      return evaluator.evaluate(board);
    }

    List<Move> moves = board.getWhiteValidMoves();
    int maxEval = Integer.MIN_VALUE;

    for (Move move : moves) {
      if (isTimeExceeded(startTime))
        break;

      undo.registerMove(PlayerColor.WHITE, move);
      board.applyMove(move);

      int evalValue = minValue(undo, board, evaluator, depth - 1, alpha, beta, startTime);

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
   * MinValue function of the Alpha-Beta MinMax algorithm with time management.
   *
   * <p>
   * This function represents the BLACK player's turn and tries to minimize the
   * evaluation score. It uses alpha-beta pruning to cut off branches that won't
   * affect
   * the final result and respects time constraints.
   *
   * @param undo      the undo/redo manager for move operations
   * @param board     the current game board state
   * @param evaluator the evaluation function to score board positions
   * @param depth     the remaining search depth
   * @param alpha     the best value that the MAX player can guarantee so far
   * @param beta      the best value that the MIN player can guarantee so far
   * @param startTime the start time for time management
   * @return the minimum evaluation score achievable from this position
   */
  private int minValue(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth,
      int alpha, int beta, long startTime) {
    if (depth == 0 || board.noPiecesLeft(PlayerColor.WHITE) || board.noPiecesLeft(PlayerColor.BLACK)
        || isTimeExceeded(startTime)) {
      return evaluator.evaluate(board);
    }

    List<Move> moves = board.getBlackValidMoves();
    int minEval = Integer.MAX_VALUE;

    for (Move move : moves) {
      if (isTimeExceeded(startTime))
        break;

      undo.registerMove(PlayerColor.BLACK, move);
      board.applyMove(move);

      int evalValue = maxValue(undo, board, evaluator, depth - 1, alpha, beta, startTime);

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
}