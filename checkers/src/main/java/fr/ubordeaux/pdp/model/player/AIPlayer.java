package fr.ubordeaux.pdp.model.player;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.evaluation.SimpleEvaluator;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.MinMaxAlphaBeta;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;

/**
 * Represents an artificial intelligence player for the Checkers game.
 *
 * <p>
 * This class extends the base Player class and integrates AI algorithms to
 * automatically
 * determine the best moves. It uses the Strategy pattern to allow different AI
 * algorithms
 * and evaluation functions to be plugged in at runtime.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Configurable AI algorithm (MinMax, Alpha-Beta, etc.)
 * <li>Pluggable evaluation function for board scoring
 * <li>Runtime algorithm switching capability
 * <li>Integration with undo/redo system
 * </ul>
 *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * AIPlayer aiPlayer = new AIPlayer("AI_Player",
 *     new MinMax(4),
 *     new SimpleEvaluator());
 * Move bestMove = aiPlayer.getBestMove(undoManager, board, PlayerColor.WHITE);
 * }</pre>
 */
public class AiPlayer extends Player {

  /** The AI algorithm used to determine the best moves. */
  private Ai algorithm;

  /** The evaluation function used to score board positions. */
  private Evaluator evaluator;

  /**
   * Creates a new AI player with the specified algorithm and evaluator.
   *
   * @param name      the name to identify the AI in the game
   * @param algorithm the AI algorithm to use for move selection
   * @param evaluator the evaluation function to score board positions
   * @throws IllegalArgumentException if algorithm or evaluator is null
   */
  public AiPlayer(String name, Ai algorithm, Evaluator evaluator) {
    super(name);
    if (algorithm == null) {
      throw new IllegalArgumentException("AI algorithm cannot be null");
    }
    if (evaluator == null) {
      throw new IllegalArgumentException("Evaluator cannot be null");
    }
    this.algorithm = algorithm;
    this.evaluator = evaluator;
  }

  public AiPlayer(String name) {
    super(name);
    this.algorithm = new MinMaxAlphaBeta(); // Initialiser d'abord
    this.evaluator = new SimpleEvaluator(); // Puis initialiser
  }

  /**
   * Determines and returns the best move for the current game state.
   *
   * <p>
   * This method delegates the move calculation to the configured AI algorithm,
   * which will analyze the board using the specified evaluation function.
   *
   * @param undo   the undo/redo manager for move operations
   * @param board  the current game board state
   * @param player the current player color
   * @return the best move according to the AI algorithm, or null if no moves are
   *         available
   * @throws IllegalArgumentException if undo, board, or player is null
   */
  public Move getBestMove(ManagerUndoRedo undo, Board board, PlayerColor player) {
    if (undo == null) {
      throw new IllegalArgumentException("Undo manager cannot be null");
    }
    if (board == null) {
      throw new IllegalArgumentException("Board cannot be null");
    }
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null");
    }
    return algorithm.getBestMove(undo, board, player, evaluator);
  }

  /**
   * Updates the AI algorithm used by this player.
   *
   * @param algorithm the new AI algorithm to use
   * @throws IllegalArgumentException if algorithm is null
   */
  public void setAlgorithm(Ai algorithm) {
    if (algorithm == null) {
      throw new IllegalArgumentException("AI algorithm cannot be null");
    }
    this.algorithm = algorithm;
  }

  /**
   * Updates the evaluation function used by this player.
   *
   * @param evaluator the new evaluation function to use
   * @throws IllegalArgumentException if evaluator is null
   */
  public void setEvaluator(Evaluator evaluator) {
    if (evaluator == null) {
      throw new IllegalArgumentException("Evaluator cannot be null");
    }
    this.evaluator = evaluator;
  }

  /**
   * Returns the current AI algorithm used by this player.
   *
   * @return the current AI algorithm
   */
  public Ai getAlgorithm() {
    return algorithm;
  }

  /**
   * Returns the current evaluation function used by this player.
   *
   * @return the current evaluation function
   */
  public Evaluator getEvaluator() {
    return evaluator;
  }
}