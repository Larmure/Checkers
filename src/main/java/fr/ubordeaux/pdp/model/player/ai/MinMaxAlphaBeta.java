package fr.ubordeaux.pdp.model.player.ai;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.util.List;

/**
 * MinMax algorithm with alpha-beta pruning for the checkers AI.
 *
 * <p>Extends the classic MinMax approach by maintaining two bounds during the tree traversal:
 *
 * <ul>
 *   <li>{@code alpha}: the best score the maximizing player (white) can already guarantee.
 *   <li>{@code beta}: the best score the minimizing player (black) can already guarantee.
 * </ul>
 *
 * <p>When {@code beta <= alpha}, the remaining siblings are pruned because they cannot influence
 * the final decision. This significantly reduces the number of nodes evaluated compared to plain
 * MinMax.
 *
 * <p>Explores the game tree in a single pass up to {@code maxDepth}. If the time limit expires
 * mid-search, the first valid move is returned as fallback.
 */
public class MinMaxAlphaBeta extends Ai {

  /**
   * Internal exception used to abort the search when the time limit is exceeded.
   *
   * <p>Stack-trace generation is disabled for performance.
   */
  private static class TimeExceededException extends RuntimeException {
    TimeExceededException() {
      super(null, null, true, false);
    }
  }

  /** Creates a MinMaxAlphaBeta AI with default depth and time settings. */
  public MinMaxAlphaBeta() {
    super();
  }

  /**
   * Creates a MinMaxAlphaBeta AI with the specified search depth and default time limit.
   *
   * @param depth the maximum search depth
   */
  public MinMaxAlphaBeta(int depth) {
    super(depth);
  }

  /**
   * Creates a MinMaxAlphaBeta AI with the specified search depth and time limit.
   *
   * @param depth the maximum search depth
   * @param maxTimeMs the maximum thinking time in milliseconds
   */
  public MinMaxAlphaBeta(int depth, long maxTimeMs) {
    super(depth, maxTimeMs);
  }

  /**
   * Calculates the best move using a single-pass MinMax search with alpha-beta pruning.
   *
   * <p>Explores the game tree up to {@code maxDepth} in one pass.
   * If the time limit expires mid-search, the first valid move is returned as fallback.
   *
   * @param undo the undo manager used to apply and revert moves
   * @param board the current state of the game board
   * @param player the color of the current player
   * @param evaluator the evaluation function used to score board positions
   * @param validMoves the list of valid moves available to the current player
   * @return the best move found within the time and depth constraints
   */
  @Override
  protected Move calculateBestMove(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> validMoves) {
    long startTime = System.currentTimeMillis();
    try {
      return findBestMoveAtDepth(undo, board, player, evaluator, validMoves, maxDepth, startTime);
    } catch (TimeExceededException e) {
      return validMoves.get(0);
    }
  }

  /**
   * Searches for the best move at {@code maxDepth} using alpha-beta pruning.
   *
   * @param undo the undo manager used to apply and revert moves
   * @param board the current state of the game board
   * @param player the color of the current player
   * @param evaluator the evaluation function used to score board positions
   * @param moves the list of candidate moves for the current player
   * @param depth the depth at which to search
   * @param startTime the timestamp marking the start of the overall search
   * @return the best move found, or {@code null} if the move list is empty
   * @throws TimeExceededException if the time limit is exceeded during the search
   */
  private Move findBestMoveAtDepth(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> moves, int depth, long startTime) {

    Move bestMove = null;
    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;

    if (player == PlayerColor.WHITE) {
      int bestValue = Integer.MIN_VALUE;
      for (Move move : moves) {
        checkTime(startTime);
        undo.registerMove(player, move);
        board.applyMove(move);
        int value;
        try {
          value = minValue(undo, board, evaluator, depth - 1, alpha, beta, startTime);
        } finally {
          undo.undo(true);
        }
        if (value > bestValue) {
          bestValue = value;
          bestMove = move;
        }
        alpha = Math.max(alpha, bestValue);
      }
    } else {
      int bestValue = Integer.MAX_VALUE;
      for (Move move : moves) {
        checkTime(startTime);
        undo.registerMove(player, move);
        board.applyMove(move);
        int value;
        try {
          value = maxValue(undo, board, evaluator, depth - 1, alpha, beta, startTime);
        } finally {
          undo.undo(false);
        }
        if (value < bestValue) {
          bestValue = value;
          bestMove = move;
        }
        beta = Math.min(beta, bestValue);
      }
    }
    return bestMove;
  }

  private int maxValue(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth,
      int alpha, int beta, long startTime) {
    if (depth == 0 || board.noPiecesLeft(PlayerColor.WHITE)
        || board.noPiecesLeft(PlayerColor.BLACK)) {
      return evaluator.evaluate(board);
    }
    List<Move> moves = board.getWhiteValidMoves();
    int maxEval = Integer.MIN_VALUE;
    for (Move move : moves) {
      checkTime(startTime);
      undo.registerMove(PlayerColor.WHITE, move);
      board.applyMove(move);
      int evalValue;
      try {
        evalValue = minValue(undo, board, evaluator, depth - 1, alpha, beta, startTime);
      } finally {
        undo.undo(true);
      }
      maxEval = Math.max(maxEval, evalValue);
      alpha = Math.max(alpha, evalValue);
      if (beta <= alpha) {
        break;
      }
    }
    return maxEval;
  }

  private int minValue(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth,
      int alpha, int beta, long startTime) {
    if (depth == 0 || board.noPiecesLeft(PlayerColor.WHITE)
        || board.noPiecesLeft(PlayerColor.BLACK)) {
      return evaluator.evaluate(board);
    }
    List<Move> moves = board.getBlackValidMoves();
    int minEval = Integer.MAX_VALUE;
    for (Move move : moves) {
      checkTime(startTime);
      undo.registerMove(PlayerColor.BLACK, move);
      board.applyMove(move);
      int evalValue;
      try {
        evalValue = maxValue(undo, board, evaluator, depth - 1, alpha, beta, startTime);
      } finally {
        undo.undo(false);
      }
      minEval = Math.min(minEval, evalValue);
      beta = Math.min(beta, evalValue);
      if (beta <= alpha) {
        break;
      }
    }
    return minEval;
  }

  private void checkTime(long startTime) {
    if (isTimeExceeded(startTime)) {
      throw new TimeExceededException();
    }
  }
}