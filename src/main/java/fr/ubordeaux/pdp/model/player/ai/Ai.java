package fr.ubordeaux.pdp.model.player.ai;

import java.util.List;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;

/**
 * Abstract base class for AI algorithms that can determine the best move in a
 * checkers game.
 *
 * <p>This class provides common constants, utility methods, and template methods
 * for all AI implementations. It defines the standard contract for AI behavior
 * while allowing concrete implementations to customize their specific
 * algorithms.
 *
 * <p>Key features:
 * <ul>
 * <li>Standardized constants for depth and time limits
 * <li>Parameter validation utilities
 * <li>Time management helpers
 * <li>Template method pattern for common AI operations
 * </ul>
 */
public abstract class Ai {

  /** Default search depth for MinMax-based algorithms. */
  protected static final int DEFAULT_DEPTH = 5; //Visuellement par defaut 12

  /** Default maximum thinking time in milliseconds (5 seconds). */
  protected static final long DEFAULT_MAX_TIME_MS = 5000L;

  /** Maximum search depth allowed to prevent stack overflow. */
  protected static final int MAX_SAFE_DEPTH = 20;

  /** Minimum thinking time in milliseconds to allow basic computation. */
  protected static final long MIN_TIME_MS = 100L;

  /** Maximum reasonable thinking time in milliseconds (30 seconds). */
  protected static final long MAX_TIME_MS = 30000L;

  /** Maximum search depth for this specific AI instance. */
  protected int maxDepth;

  /** Maximum thinking time for this specific AI instance. */
  protected long maxTimeMs;

  /**
   * Creates an AI with default depth and time settings.
   */
  protected Ai() {
    this(DEFAULT_DEPTH, DEFAULT_MAX_TIME_MS);
  }

  /**
   * Creates an AI with specified depth and default time.
   *
   * @param maxDepth the maximum search depth
   */
  protected Ai(int maxDepth) {
    this(maxDepth, DEFAULT_MAX_TIME_MS);
  }

  /**
   * Creates an AI with specified depth and time.
   *
   * @param maxDepth  the maximum search depth
   * @param maxTimeMs the maximum thinking time in milliseconds
   */
  protected Ai(int maxDepth, long maxTimeMs) {
    validateDepth(maxDepth);
    validateTime(maxTimeMs);
    this.maxDepth = maxDepth;
    this.maxTimeMs = maxTimeMs;
  }

  /**
   * Determines and returns the best move for the current game state.
   *
   * <p>This is the main entry point for AI decision making. It validates
   * parameters and delegates to the concrete implementation.
   *
   * @param undo      the undo/redo manager for move operations
   * @param board     the current game board state
   * @param player    the current player color
   * @param evaluator the evaluation function to score board positions
   * @return the best move according to the AI algorithm, or null if no moves are
   *         available
   * @throws IllegalArgumentException if any parameter is null
   */
  public final Move getBestMove(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator) {
    validateParameters(undo, board, player, evaluator);

    List<Move> validMoves = getValidMoves(board, player);
    if (validMoves.isEmpty()) {
      return null;
    }

    return calculateBestMove(undo, board, player, evaluator, validMoves);
  }

  /**
   * Template method for calculating the best move.
   *
   * <p>Concrete implementations must override this method to provide their
   * specific algorithm for finding the best move.
   *
   * @param undo       the undo/redo manager for move operations
   * @param board      the current game board state
   * @param player     the current player color
   * @param evaluator  the evaluation function to score board positions
   * @param validMoves list of valid moves for the current player
   * @return the best move according to the AI algorithm
   */
  protected abstract Move calculateBestMove(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> validMoves);

  /**
   * Gets the valid moves for the specified player.
   *
   * @param board  the current game board
   * @param player the player color
   * @return list of valid moves
   */
  protected List<Move> getValidMoves(Board board, PlayerColor player) {
    return player == PlayerColor.WHITE ? board.getWhiteValidMoves() : board.getBlackValidMoves();
  }

  /**
   * Validates that the specified depth is within acceptable bounds.
   *
   * @param depth the depth to validate
   * @throws IllegalArgumentException if depth is invalid
   */
  protected final void validateDepth(int depth) {
    if (depth <= 0) {
      throw new IllegalArgumentException("Depth must be positive, got: " + depth);
    }
    if (depth > MAX_SAFE_DEPTH) {
      throw new IllegalArgumentException("Depth too large (max " + MAX_SAFE_DEPTH + "), got: "
          + depth);
    }
  }

  /**
   * Validates that the specified time is within acceptable bounds.
   *
   * @param timeMs the time in milliseconds to validate
   * @throws IllegalArgumentException if time is invalid
   */
  protected final void validateTime(long timeMs) {
    if (timeMs < MIN_TIME_MS) {
      throw new IllegalArgumentException("Time too small (min " + MIN_TIME_MS + "ms), got: "
          + timeMs);
    }
    if (timeMs > MAX_TIME_MS) {
      throw new IllegalArgumentException("Time too large (max " + MAX_TIME_MS + "ms), got: "
          + timeMs);
    }
  }

  /**
   * Validates input parameters for getBestMove.
   *
   * @param undo      the undo/redo manager
   * @param board     the game board
   * @param player    the player color
   * @param evaluator the evaluation function
   * @throws IllegalArgumentException if any parameter is null
   */
  private void validateParameters(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator) {
    if (undo == null) {
      throw new IllegalArgumentException("Undo manager cannot be null");
    }
    if (board == null) {
      throw new IllegalArgumentException("Board cannot be null");
    }
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null");
    }
    if (evaluator == null) {
      throw new IllegalArgumentException("Evaluator cannot be null");
    }
  }

  /**
   * Checks if the time limit has been exceeded.
   *
   * @param startTime the start time in milliseconds
   * @return true if time limit exceeded, false otherwise
   */
  protected final boolean isTimeExceeded(long startTime) {
    return (System.currentTimeMillis() - startTime) >= maxTimeMs;
  }

  /**
   * Gets the remaining time before timeout.
   *
   * @param startTime the start time in milliseconds
   * @return remaining time in milliseconds, or 0 if exceeded
   */
  protected final long getRemainingTime(long startTime) {
    long elapsed = System.currentTimeMillis() - startTime;
    return Math.max(0, maxTimeMs - elapsed);
  }

  // Getters et setters
  /**
   * Returns the maximum search depth for this AI instance.
   *
   * @return the maximum search depth
   */
  public int getMaxDepth() {
    return maxDepth;
  }

  /**
   * Returns the maximum thinking time in milliseconds for this AI instance.
   *
   * @return the maximum thinking time in milliseconds
   */
  public long getMaxTimeMs() {
    return maxTimeMs;
  }

  /**
   * Sets the maximum search depth for this AI instance.
   *
   * @param maxDepth the maximum search depth to set
   */
  public void setMaxDepth(int maxDepth) {
    validateDepth(maxDepth);
    this.maxDepth = maxDepth;
  }

  /**
   * Sets the maximum thinking time in milliseconds for this AI instance.
   *
   * @param maxTimeMs the maximum thinking time in milliseconds to set
   */
  public void setMaxTimeMs(long maxTimeMs) {
    validateTime(maxTimeMs);
    this.maxTimeMs = maxTimeMs;
  }
}