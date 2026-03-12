package fr.ubordeaux.pdp.model.evaluation;

import fr.ubordeaux.pdp.model.core.Board;

/**
 * Interface for board position evaluation in checkers AI.
 *
 * <p>Implementations of this interface provide different strategies for evaluating
 * the strength of a board position from the perspective of the white player.
 * Evaluators are used by AI algorithms to determine the best move by assigning
 * numerical scores to board positions.
 *
 * <p>Scoring conventions:
 * <ul>
 * <li>Positive scores indicate positions favorable to white
 * <li>Negative scores indicate positions favorable to black
 * <li>Zero indicates a balanced position
 * <li>Higher absolute values indicate stronger advantages
 * </ul>
 *
 * <p>Common evaluation factors include:
 * <ul>
 * <li>Material count (number and type of pieces)
 * <li>Positional advantages (center control, piece advancement)
 * <li>Mobility (number of legal moves available)
 * <li>Tactical considerations (captures, threats)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Evaluator evaluator = new AdvancedEvaluator();
 * int score = evaluator.evaluate(board);
 * if (score > 0) {
 *   // White has the advantage
 * } else if (score < 0) {
 *   // Black has the advantage
 * }
 * }</pre>
 */
public interface Evaluator {
  /**
   * Evaluates the current board position and returns a numerical score.
   *
   * <p>The evaluation should consider all relevant strategic and tactical factors
   * to provide an accurate assessment of the position's strength. The scoring
   * system should be consistent across multiple evaluations of similar positions.
   *
   * <p>Implementations should be efficient as this method may be called frequently
   * during AI search operations.
   *
   * @param board the current board position to evaluate; must not be null
   * @return the evaluation score:
   *         <ul>
   *         <li>Positive: white advantage
   *         <li>Negative: black advantage
   *         <li>Zero: balanced position
   *         </ul>
   * @throws IllegalArgumentException if board is null
   */
  int evaluate(Board board);
}