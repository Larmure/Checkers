package fr.ubordeaux.pdp.model.evaluation;

import fr.ubordeaux.pdp.model.core.Board;

/**
 * Simple implementation of board evaluation for checkers AI.
 *
 * <p>
 * This evaluator uses a basic material-based scoring system where:
 * <ul>
 * <li>White pieces contribute positive scores
 * <li>Black pieces contribute negative scores
 * <li>Checkers (kings) are worth more than pawns
 * </ul>
 *
 * <p>
 * The scoring is straightforward:
 * <ul>
 * <li>Pawn: 100 points
 * <li>Checker: 350 points
 * </ul>
 *
 * <p>
 * This evaluator provides a baseline evaluation that focuses purely on material
 * advantage without considering positional factors.
 */
public class SimpleEvaluator implements Evaluator {

  /** Point value for a pawn (basic piece). */
  private static final int PAWN_VALUE = 100;

  /** Point value for a checker (king piece). */
  private static final int CHECKER_VALUE = 350;

  /**
   * Evaluates the board position using simple material counting.
   *
   * <p>
   * The evaluation is calculated as:
   * 
   * <pre>{@code
   * score = (white_pawns * PAWN_VALUE) + (white_checkers * CHECKER_VALUE)
   *     - (black_pawns * PAWN_VALUE) - (black_checkers * CHECKER_VALUE)
   * }</pre>
   *
   * <p>
   * A positive score favors white, while a negative score favors black.
   *
   * @param board the current board position to evaluate
   * @return the evaluation score (positive for white advantage, negative for
   *         black)
   */
  @Override
  public int evaluate(Board board) {
    int score = 0;

    for (int i = 0; i < board.getIndexMax(); i++) {
      if (board.isBitWhitePawn(i)) {
        score += PAWN_VALUE;
      }
      if (board.isBitWhiteChecker(i)) {
        score += CHECKER_VALUE;
      }
      if (board.isBitBlackPawn(i)) {
        score -= PAWN_VALUE;
      }
      if (board.isBitBlackChecker(i)) {
        score -= CHECKER_VALUE;
      }
    }

    return score;
  }
}