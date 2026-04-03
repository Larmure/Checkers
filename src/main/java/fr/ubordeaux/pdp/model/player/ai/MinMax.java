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
 * <p>Explores the game tree up to a fixed depth in a single pass.
 * If the time limit is exceeded mid-search, a {@link TimeExceededException} is thrown
 * and the first valid move is returned as fallback.
 */
public class MinMax extends Ai {

  private static class TimeExceededException extends RuntimeException {
    TimeExceededException() {
      super(null, null, true, false);
    }
  }

  public MinMax() {
    super();
  }

  public MinMax(int depth) {
    super(depth);
  }

  public MinMax(int depth, long maxTimeMs) {
    super(depth, maxTimeMs);
  }

  /**
   * Calculates the best move using a single-pass MinMax search up to {@code maxDepth}.
   * Returns the first valid move as fallback if time expires.
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

  private Move findBestMoveAtDepth(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> moves, int depth, long startTime) {
    Move bestMove = null;
    if (player == PlayerColor.WHITE) {
      int bestValue = Integer.MIN_VALUE;
      for (Move move : moves) {
        checkTime(startTime);
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

  private void checkTime(long startTime) {
    if (isTimeExceeded(startTime)) {
      throw new TimeExceededException();
    }
  }
}