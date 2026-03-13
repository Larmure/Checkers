package fr.ubordeaux.pdp.model.player.ai;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.util.List;

/**
 * Implementation of the MinMax algorithm for checkers AI.
 */
public class MinMax extends Ai {

  /**
   * Creates a MinMax AI with default depth and time settings.
    */
  public MinMax() {
    super();
  }

  /**
   * Creates a MinMax AI with specified depth and default time.
   *
   * @param depth the maximum search depth
   */
  public MinMax(int depth) {
    super(depth);
  }

  /**
   * Creates a MinMax AI with specified depth and time.
   *
   * @param depth     the maximum search depth
   * @param maxTimeMs the maximum thinking time in milliseconds
   */
  public MinMax(int depth, long maxTimeMs) {
    super(depth, maxTimeMs);
  }

  /**
   * Calculates the best move using the MinMax algorithm with alpha-beta pruning.
   *
   * @param undo      the undo manager to handle move registration and undoing
   * @param board     the current state of the game board
   * @param player    the color of the current player
   * @param evaluator the evaluation function to score board positions
   * @param validMoves the list of valid moves for the current player
   * @return the best move according to the MinMax algorithm, or null if no moves
   *         are available
   */
  @Override
  protected Move calculateBestMove(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> validMoves) {

    long startTime = System.currentTimeMillis();
    Move bestMove = validMoves.get(0);
    int currentDepth = 1;

    // Recherche itérative avec limite de temps
    while (currentDepth <= maxDepth && !isTimeExceeded(startTime)) {
      Move tempBestMove = findBestMoveAtDepth(undo, board, player, evaluator, validMoves,
          currentDepth, startTime);

      if (tempBestMove != null) {
        bestMove = tempBestMove;
      }
      currentDepth++;
    }

    return bestMove;
  }

  /**
   * Helper method to find the best move at a specific search depth using MinMax.
   *
   * @param undo     the undo manager to handle move registration and undoing
   * @param board    the current state of the game board
   * @param player  the color of the current player
   * @param evaluator the evaluation function to score board positions
   * @param moves the list of valid moves for the current player
   * @param depth    the current search depth
   * @param startTime the time when the search started, used for time limit checks
   * @return the best move found at the specified depth, or null if no moves are
   *         available
   */
  private Move findBestMoveAtDepth(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> moves, int depth, long startTime) {

    Move bestMove = null;

    if (player == PlayerColor.WHITE) {
      int bestValue = Integer.MIN_VALUE;
      for (Move move : moves) {
        if (isTimeExceeded(startTime)) {
          break;
        }

        undo.registerMove(player, move);
        board.applyMove(move);

        int value = minMax(undo, board, evaluator, depth - 1, startTime);

        undo.undo(true);

        if (value > bestValue) {
          bestValue = value;
          bestMove = move;
        }
      }
    } else {
      int bestValue = Integer.MAX_VALUE;
      for (Move move : moves) {
        if (isTimeExceeded(startTime)) {
          break;
        }

        undo.registerMove(player, move);
        board.applyMove(move);

        int value = maxMin(undo, board, evaluator, depth - 1, startTime);

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
   * MinMax function for the minimizing player (black).
   *
   * <p>Recursively evaluates the game tree, returning the minimum score for the
   * minimizing player. This function assumes the maximizing player (white) will
   * choose the move that maximizes their score, while the minimizing player will
   * choose the move that minimizes their score.
   *
   * @param undo      the undo manager to handle move registration and undoing
   * @param board     the current state of the game board
   * @param evaluator the evaluation function to score board positions
   * @param depth     the current search depth
   * @param startTime the time when the search started, used for time limit checks
   * @return the minimum score for the minimizing player at this node
   */
  private int minMax(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth,
      long startTime) {
    if (depth == 0 || board.noPiecesLeft(PlayerColor.WHITE) || board.noPiecesLeft(PlayerColor.BLACK)
        || isTimeExceeded(startTime)) {
      return evaluator.evaluate(board);
    }

    List<Move> moves = board.getBlackValidMoves();
    int minEval = Integer.MAX_VALUE;

    for (Move move : moves) {
      if (isTimeExceeded(startTime)) {
        break;
      }

      undo.registerMove(PlayerColor.BLACK, move);
      board.applyMove(move);

      int evalValue = maxMin(undo, board, evaluator, depth - 1, startTime);

      undo.undo(false);

      minEval = Math.min(minEval, evalValue);
    }

    return minEval;
  }

  /**
   * MinMax function for the maximizing player (white).
   *
   * @param undo      the undo manager to handle move registration and undoing
   * @param board     the current state of the game board
   * @param evaluator the evaluation function to score board positions
   * @param depth     the current search depth
   * @param startTime the time when the search started, used for time limit checks
   * @return the maximum score for the maximizing player at this node
   */
  private int maxMin(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth,
      long startTime) {
    if (depth == 0 || board.noPiecesLeft(PlayerColor.WHITE) || board.noPiecesLeft(PlayerColor.BLACK)
        || isTimeExceeded(startTime)) {
      return evaluator.evaluate(board);
    }

    List<Move> moves = board.getWhiteValidMoves();
    int maxEval = Integer.MIN_VALUE;

    for (Move move : moves) {
      if (isTimeExceeded(startTime)) {
        break;
      }

      undo.registerMove(PlayerColor.WHITE, move);
      board.applyMove(move);

      int evalValue = minMax(undo, board, evaluator, depth - 1, startTime);

      undo.undo(true);

      maxEval = Math.max(maxEval, evalValue);
    }

    return maxEval;
  }
}