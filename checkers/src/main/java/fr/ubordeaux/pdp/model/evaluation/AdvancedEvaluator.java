package fr.ubordeaux.pdp.model.evaluation;

import fr.ubordeaux.pdp.model.core.Board;

/**
 * Advanced implementation of board evaluation for checkers AI.
 *
 * <p>
 * This evaluator combines material counting with positional factors:
 * <ul>
 * <li>Material advantage (pawns and checkers)
 * <li>Pawn advancement (closer to promotion is better)
 * <li>Asymmetric scoring for white and black advancement
 * </ul>
 *
 * <p>
 * The scoring factors are:
 * <ul>
 * <li>Pawn: 100 points
 * <li>Checker: 350 points
 * <li>Advancement bonus: 8 points per row
 * </ul>
 *
 * <p>
 * White pawns gain advancement bonuses for moving forward (higher row indices),
 * while black pawns gain bonuses for moving forward (lower row indices).
 */
public class AdvancedEvaluator implements Evaluator {

  /** Point value for a pawn (basic piece). */
  private static final int PAWN_VALUE = 100;

  /** Point value for a checker (king piece). */
  private static final int CHECKER_VALUE = 350;

  /** Bonus points per row of pawn advancement. */
  private static final int ADVANCEMENT_BONUS = 8;

  /**
   * Evaluates the board position using material and positional factors.
   *
   * <p>
   * The evaluation considers:
   * <ul>
   * <li>Material count (pawns and checkers)
   * <li>White pawn advancement: bonus increases with row index
   * <li>Black pawn advancement: bonus increases as row index decreases
   * </ul>
   *
   * <p>
   * Formula:
   * 
   * <pre>{@code
   // For white pieces
   score += PAWN_VALUE + (row_index * ADVANCEMENT_BONUS)  // pawns
   score += CHECKER_VALUE                                 // checkers
   
   // For black pieces  
   score -= PAWN_VALUE + ((board_size - 1 - row_index) * ADVANCEMENT_BONUS)  // pawns
   score -= CHECKER_VALUE                                 // checkers
   }</pre>
   *
   * @param board the current board position to evaluate
   * @return the evaluation score (positive for white advantage, negative for
   *         black)
   */
  @Override
  public int evaluate(Board board) {
    int score = 0;
    int halfBoard = board.getSizeBoard() / 2;

    for (int i = 0; i < board.getIndexMax(); i++) {
      int row = i / halfBoard;

      if (board.isBitWhitePawn(i)) {
        score += PAWN_VALUE;
        score += row * ADVANCEMENT_BONUS;
      }

      if (board.isBitWhiteChecker(i)) {
        score += CHECKER_VALUE;
      }

      if (board.isBitBlackPawn(i)) {
        score -= PAWN_VALUE;
        score -= (board.getSizeBoard() - 1 - row) * ADVANCEMENT_BONUS;
      }

      if (board.isBitBlackChecker(i)) {
        score -= CHECKER_VALUE;
      }
    }

    return score;
  }
}