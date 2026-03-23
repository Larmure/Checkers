package fr.ubordeaux.pdp.model.player.ai;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.util.List;

/**
 * MinMax algorithm implementation for the checkers AI.
 *
 * <p>Uses iterative deepening to progressively explore the game tree one depth level at a time.
 * When the time limit is reached mid-depth, the result of the last fully completed depth is
 * returned rather than a partial, potentially unreliable result.
 */
public class MinMax extends Ai {

  /**
   * Internal exception used to abort a depth search when the time limit is exceeded.
   *
   * <p>Thrown by {@link #checkTime(long)} and propagated up through the recursive calls to
   * invalidate the current depth's partial result. Stack-trace generation is disabled for
   * performance.
   */
  private static class TimeExceededException extends RuntimeException {
    TimeExceededException() {
      super(null, null, true, false);
    }
  }

  /** Creates a MinMax AI with default depth and time settings. */
  public MinMax() {
    super();
  }

  /**
   * Creates a MinMax AI with the specified search depth and default time limit.
   *
   * @param depth the maximum search depth
   */
  public MinMax(int depth) {
    super(depth);
  }

  /**
   * Creates a MinMax AI with the specified search depth and time limit.
   *
   * @param depth the maximum search depth
   * @param maxTimeMs the maximum thinking time in milliseconds
   */
  public MinMax(int depth, long maxTimeMs) {
    super(depth, maxTimeMs);
  }

  /**
   * Calculates the best move using iterative-deepening MinMax.
   *
   * <p>Explores the game tree depth by depth, from 1 up to {@code maxDepth}. After each fully
   * completed depth, the best move found is retained. If the time limit expires during a depth
   * search, a {@link TimeExceededException} is thrown, the partial result is discarded, and the
   * best move from the previous completed depth is returned.
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
    // Fallback to the first move; updated after each fully completed depth.
    Move bestMove = validMoves.get(0);

    for (int currentDepth = 1; currentDepth <= maxDepth; currentDepth++) {
      if (isTimeExceeded(startTime)) {
        break;
      }

      try {
        Move candidate = findBestMoveAtDepth(undo, board, player, evaluator, validMoves,
            currentDepth, startTime);
        // Depth fully explored: accept the result.
        if (candidate != null) {
          bestMove = candidate;
        }
      } catch (TimeExceededException e) {
        // Depth incomplete: discard the partial result and keep the previous best move.
        break;
      }
    }

    return bestMove;
  }

  /**
   * Searches for the best move at the given depth using MinMax.
   *
   * <p>Iterates over all candidate moves, applying and undoing each one while recursively
   * evaluating the resulting position. The {@code finally} block guarantees that {@code undo} is
   * always called even if a {@link TimeExceededException} propagates up mid-search, keeping the
   * board state consistent.
   *
   * @param undo the undo manager used to apply and revert moves
   * @param board the current state of the game board
   * @param player the color of the current player
   * @param evaluator the evaluation function used to score board positions
   * @param moves the list of candidate moves for the current player
   * @param depth the depth at which to search
   * @param startTime the timestamp marking the start of the overall search
   * @return the best move found at this depth, or {@code null} if the move list is empty
   * @throws TimeExceededException if the time limit is exceeded during the search
   */
  private Move findBestMoveAtDepth(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> moves, int depth, long startTime) {

    Move bestMove = null;

    if (player == PlayerColor.WHITE) {
      int bestValue = Integer.MIN_VALUE;
      for (Move move : moves) {
        checkTime(startTime); // throws TimeExceededException if time is up

        undo.registerMove(player, move);
        board.applyMove(move);

        int value;
        try {
          value = minMax(undo, board, evaluator, depth - 1, startTime);
        } finally {
          undo.undo(true);
        }

        if (value > bestValue) {
          bestValue = value;
          bestMove = move;
        }
      }
    } else {
      int bestValue = Integer.MAX_VALUE;
      for (Move move : moves) {
        checkTime(startTime);

        undo.registerMove(player, move);
        board.applyMove(move);

        int value;
        try {
          value = maxMin(undo, board, evaluator, depth - 1, startTime);
        } finally {
          undo.undo(false);
        }

        if (value < bestValue) {
          bestValue = value;
          bestMove = move;
        }
      }
    }

    return bestMove;
  }

  /**
   * Evaluates the game tree from the perspective of the minimizing player (black).
   *
   * <p>Recursively explores all moves available to black and returns the minimum board score,
   * assuming white will respond optimally via {@link #maxMin}. Evaluation is cut off when
   * {@code depth} reaches zero or one side has no pieces remaining.
   *
   * @param undo the undo manager used to apply and revert moves
   * @param board the current state of the game board
   * @param evaluator the evaluation function used to score board positions
   * @param depth the remaining search depth
   * @param startTime the timestamp marking the start of the overall search
   * @return the minimum board score reachable from the current position
   * @throws TimeExceededException if the time limit is exceeded during the search
   */
  private int minMax(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth,
      long startTime) {
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
        evalValue = maxMin(undo, board, evaluator, depth - 1, startTime);
      } finally {
        undo.undo(false);
      }

      minEval = Math.min(minEval, evalValue);
    }

    return minEval;
  }

  /**
   * Evaluates the game tree from the perspective of the maximizing player (white).
   *
   * <p>Recursively explores all moves available to white and returns the maximum board score,
   * assuming black will respond optimally via {@link #minMax}. Evaluation is cut off when
   * {@code depth} reaches zero or one side has no pieces remaining.
   *
   * @param undo the undo manager used to apply and revert moves
   * @param board the current state of the game board
   * @param evaluator the evaluation function used to score board positions
   * @param depth the remaining search depth
   * @param startTime the timestamp marking the start of the overall search
   * @return the maximum board score reachable from the current position
   * @throws TimeExceededException if the time limit is exceeded during the search
   */
  private int maxMin(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth,
      long startTime) {
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
        evalValue = minMax(undo, board, evaluator, depth - 1, startTime);
      } finally {
        undo.undo(true);
      }

      maxEval = Math.max(maxEval, evalValue);
    }

    return maxEval;
  }

  /**
   * Throws {@link TimeExceededException} if the allotted thinking time has elapsed.
   *
   * <p>Unlike {@link #isTimeExceeded(long)}, which returns a boolean, this method aborts the
   * current depth search immediately via an unchecked exception, ensuring no partial result is
   * silently accepted.
   *
   * @param startTime the timestamp marking the start of the overall search
   * @throws TimeExceededException if the time limit has been reached
   */
  private void checkTime(long startTime) {
    if (isTimeExceeded(startTime)) {
      throw new TimeExceededException();
    }
  }
}