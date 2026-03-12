package fr.ubordeaux.pdp.model.evaluation;

import fr.ubordeaux.pdp.model.core.Board;

/**
 * Maximum implementation of board evaluation for checkers AI.
 *
 * <p>
 * This evaluator provides the most comprehensive analysis by combining:
 * <ul>
 * <li>Material advantage (pawns and checkers)
 * <li>Pawn advancement incentives
 * <li>Center control bonuses
 * <li>Mobility (number of legal moves)
 * </ul>
 *
 * <p>
 * The scoring factors are:
 * <ul>
 * <li>Pawn: 100 points
 * <li>Checker: 350 points
 * <li>Advancement: 8 points per row
 * <li>Mobility: 5 points per legal move
 * <li>Center control: 15 points per center piece
 * </ul>
 *
 * <p>
 * This evaluator is designed for stronger AI play by considering multiple
 * strategic aspects of checkers gameplay.
 */
public class MaxEvaluator implements Evaluator {

  /** Point value for a pawn (basic piece). */
  private static final int PAWN_VALUE = 100;

  /** Point value for a checker (king piece). */
  private static final int CHECKER_VALUE = 350;

  /** Bonus points per row of pawn advancement. */
  private static final int ADVANCEMENT_BONUS = 8;

  /** Points per legal move (mobility factor). */
  private static final int MOBILITY_BONUS = 5;

  /** Bonus points for controlling center squares. */
  private static final int CENTER_CONTROL_BONUS = 15;

  /**
   * Evaluates the board position using comprehensive strategic factors.
   *
   * <p>
   * The evaluation combines multiple strategic elements:
   * <ul>
   * <li><strong>Material:</strong> Standard pawn/checker values
   * <li><strong>Advancement:</strong> Progress toward promotion
   * <li><strong>Center Control:</strong> Pieces in central positions get bonuses
   * <li><strong>Mobility:</strong> More legal moves indicate better position
   * </ul>
   *
   * <p>
   * Center is defined as the 4x4 area in the middle of the board.
   * Pieces in this area receive additional bonuses for controlling key squares.
   *
   * @param board the current board position to evaluate
   * @return the evaluation score (positive for white advantage, negative for
   *         black)
   */
  @Override
  public int evaluate(Board board) {
    int score = 0;
    int boardSize = board.getSizeBoard();
    int halfBoard = boardSize / 2;

    // Material and positional evaluation
    for (int i = 0; i < board.getIndexMax(); i++) {
      int row = i / halfBoard;
      int col = (i % halfBoard) * 2 + (row % 2 == 0 ? 0 : 1);

      // Material and advancement
      if (board.isBitWhitePawn(i)) {
        score += PAWN_VALUE;
        score += row * ADVANCEMENT_BONUS;
      }

      if (board.isBitWhiteChecker(i)) {
        score += CHECKER_VALUE;
      }

      if (board.isBitBlackPawn(i)) {
        score -= PAWN_VALUE;
        score -= (boardSize - 1 - row) * ADVANCEMENT_BONUS;
      }

      if (board.isBitBlackChecker(i)) {
        score -= CHECKER_VALUE;
      }

      // Center control bonus
      if (isInCenter(row, col, boardSize)) {
        if (board.isBitWhitePawn(i) || board.isBitWhiteChecker(i)) {
          score += CENTER_CONTROL_BONUS;
        }
        if (board.isBitBlackPawn(i) || board.isBitBlackChecker(i)) {
          score -= CENTER_CONTROL_BONUS;
        }
      }
    }

    // Mobility evaluation
    score += board.getWhiteValidMoves().size() * MOBILITY_BONUS;
    score -= board.getBlackValidMoves().size() * MOBILITY_BONUS;

    return score;
  }

  /**
   * Determines if a position is in the center area of the board.
   *
   * <p>
   * Center is defined as the central 4x4 squares for standard board sizes.
   * This area is strategically important as it provides more movement options
   * and control over the game flow.
   *
   * @param row       the row index (0-based)
   * @param col       the column index (0-based)
   * @param boardSize the total size of the board
   * @return true if the position is in the center area
   */
  private boolean isInCenter(int row, int col, int boardSize) {
    int centerStart = boardSize / 2 - 2;
    int centerEnd = boardSize / 2 + 1;

    return row >= centerStart && row <= centerEnd
        && col >= centerStart && col <= centerEnd;
  }
}