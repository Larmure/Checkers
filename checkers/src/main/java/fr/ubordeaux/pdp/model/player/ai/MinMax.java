package fr.ubordeaux.pdp.model.player.ai;

import java.util.List;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;

public class MinMax extends Ai {

  public MinMax() {
    super();
  }

  public MinMax(int depth) {
    super(depth);
  }

  public MinMax(int depth, long maxTimeMs) {
    super(depth, maxTimeMs);
  }

  @Override
  protected Move calculateBestMove(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> validMoves) {

    long startTime = System.currentTimeMillis();
    Move bestMove = validMoves.get(0);
    int currentDepth = 1;

    // Recherche itérative avec limite de temps
    while (currentDepth <= maxDepth && !isTimeExceeded(startTime)) {
      Move tempBestMove = findBestMoveAtDepth(undo, board, player, evaluator, validMoves, currentDepth, startTime);

      if (tempBestMove != null) {
        bestMove = tempBestMove;
      }
      currentDepth++;
    }

    return bestMove;
  }

  private Move findBestMoveAtDepth(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> moves, int depth, long startTime) {

    Move bestMove = null;

    if (player == PlayerColor.WHITE) {
      int bestValue = Integer.MIN_VALUE;
      for (Move move : moves) {
        if (isTimeExceeded(startTime))
          break;

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
        if (isTimeExceeded(startTime))
          break;

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

  private int minMax(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth, long startTime) {
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

      int evalValue = maxMin(undo, board, evaluator, depth - 1, startTime);

      undo.undo(false);

      minEval = Math.min(minEval, evalValue);
    }

    return minEval;
  }

  private int maxMin(ManagerUndoRedo undo, Board board, Evaluator evaluator, int depth, long startTime) {
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

      int evalValue = minMax(undo, board, evaluator, depth - 1, startTime);

      undo.undo(true);

      maxEval = Math.max(maxEval, evalValue);
    }

    return maxEval;
  }
}