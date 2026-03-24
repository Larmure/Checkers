
package fr.ubordeaux.pdp.model.core;

import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.tools.Utils;

/**
 * Configuration class that encapsulates all the settings for a game of
 * checkers.
 * It includes options for blitz mode, time limits, contest mode, board size,
 * verbosity, debug mode, and whether each player is controlled by AI.
 * The class provides constructors for creating configurations with specific
 * settings, as well as a static method to get the default configuration. It
 * also includes getter methods for each configuration option and a toString
 * method for easy representation of the configuration state.
 *
 * @version 1.0
 */
public class Configuration {
  /** Indicates whether the game is in blitz mode. */
  private final boolean blitz;
  /** The time limit for each player in seconds. */
  private final int time;
  /** Indicates whether the game is in contest mode. */
  private final boolean contest;
  /** The size of the game board. */
  private final int size;
  /** Indicates whether verbose output is enabled. */
  private final boolean verbose;
  /** Indicates whether debug mode is enabled. */
  private final boolean debug;
  /** Indicates whether the white player is controlled by AI. */
  private final boolean whiteAi;
  /** Indicates whether the black player is controlled by AI. */
  private final boolean blackAi;
  /** The time limit for AI moves in seconds. */
  private final long aiTime;
  /** The mode for the AI. */
  private final String aiMode;
  /** The search depth for the AI. */
  private final int aiDepth;

  /**
   * Constructs a Configuration object with the specified settings. It validates
   * the
   * input parameters and provides warnings if certain options are used
   * incorrectly
   * (e.g., time option without blitz). The constructor ensures that the
   * configuration
   * is consistent and adheres to the expected constraints for a checkers game.
   *
   * @param blitz   Indicates whether the game is in blitz mode, which imposes
   *                time limits on players.
   * @param time    The time limit for each player in seconds, applicable only
   *                if blitz mode is enabled.
   * @param contest Indicates whether the game is in contest mode, which may
   *                affect scoring and rules.
   * @param size    The size of the game board, which must be one of the valid
   *                sizes defined in Utils.VALID_SIZES.
   * @param verbose Indicates whether verbose output is enabled, providing more
   *                detailed information during the game.
   * @param debug   Indicates whether debug mode is enabled, which may include
   *                additional logging for troubleshooting purposes.
   * @param whiteAi Indicates whether the white player is controlled by AI.
   * @param blackAi Indicates whether the black player is controlled by AI.
   * @param aiTime  The time limit for AI moves in milliseconds.
   * @param aiMode  The mode for the AI.
   * @param aiDepth The search depth for the AI.
   */
  public Configuration(boolean blitz, int time, boolean contest, int size,
      boolean verbose, boolean debug, boolean whiteAi, boolean blackAi, long aiTime,
      String aiMode, int aiDepth) {
    if (!blitz && time != Utils.DEFAULT_TIME) {
      System.out.println("Warning: time option used without blitz option.");
      blitz = Utils.DEFAULT_BLITZ;
      time = Utils.DEFAULT_TIME;
    }
    if (!Utils.VALID_SIZES.contains(size)) {
      System.out.println("Warning: Invalid board size, changed to "
          + Utils.DEFAULT_BOARD_SIZE + ".");
      size = Utils.DEFAULT_BOARD_SIZE;
    }
    if (!Utils.VALID_AI_MODES.contains(aiMode)) {
      System.out.println("Warning: Invalid AI mode, changed to "
          + Utils.DEFAULT_AI_MODE + ".");
      aiMode = Utils.DEFAULT_AI_MODE;
    }

    this.blitz = blitz;
    this.time = time;
    this.contest = contest;
    this.size = size;
    this.verbose = verbose;
    this.debug = debug;
    this.whiteAi = whiteAi;
    this.blackAi = blackAi;
    this.aiTime = aiTime;
    this.aiMode = aiMode;
    this.aiDepth = aiDepth;
  }

  /**
   * Copy constructor that creates a new Configuration object by copying the
   * values
   * from another Configuration instance. This allows for creating a new
   * configuration
   * based on an existing one, with the option to modify specific settings if
   * needed.
   *
   * @param other The Configuration instance to copy from.
   */
  public Configuration(Configuration other) {
    this.blitz = other.blitz;
    this.time = other.time;
    this.contest = other.contest;
    this.size = other.size;
    this.verbose = other.verbose;
    this.debug = other.debug;
    this.whiteAi = other.whiteAi;
    this.blackAi = other.blackAi;
    this.aiTime = other.aiTime;
    this.aiMode = other.aiMode;
    this.aiDepth = other.aiDepth;
  }

  /**
   * Copy constructor that creates a new Configuration object by copying the
   * values.
   *
   * @param other   The Configuration instance to copy from.
   * @param verbose The verbose setting for the new Configuration.
   * @param debug   The debug setting for the new Configuration.
   */
  public Configuration(Configuration other, boolean verbose, boolean debug) {
    this.blitz = other.blitz;
    this.time = other.time;
    this.contest = other.contest;
    this.size = other.size;
    this.verbose = verbose;
    this.debug = debug;
    this.whiteAi = other.whiteAi;
    this.blackAi = other.blackAi;
    this.aiTime = other.aiTime;
    this.aiMode = other.aiMode;
    this.aiDepth = other.aiDepth;
  }

  /**
   * Static method to get the default configuration for a checkers game. This
   * configuration is based on the default values defined in the Utils class and
   * provides a standard setup for players who do not specify custom settings.
   *
   * @return A Configuration object initialized with default settings.
   */
  public static Configuration getDefaultConfiguration() {
    return new Configuration(Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME,
        Utils.DEFAULT_CONTEST, Utils.DEFAULT_BOARD_SIZE,
        Utils.DEFAULT_VERBOSE, Utils.DEFAULT_DEBUG, Utils.DEFAULT_WHITE_AI,
        Utils.DEFAULT_BLACK_AI, Ai.DEFAULT_MAX_TIME_MS, Utils.DEFAULT_AI_MODE,
        Ai.DEFAULT_DEPTH);
  }

  /**
   * Returns whether the game is in blitz mode, which imposes time limits on
   * players.
   *
   * @return true if the game is in blitz mode, false otherwise.
   */
  public boolean isBlitz() {
    return blitz;
  }

  /**
   * Returns the time limit for each player in seconds, applicable only if blitz
   * mode is enabled.
   *
   * @return the time limit for each player in seconds.
   */
  public int getTime() {
    return time;
  }

  /**
   * Returns whether the game is in contest mode, which may affect scoring and
   * rules.
   *
   * @return true if the game is in contest mode, false otherwise.
   */
  public boolean isContest() {
    return contest;
  }

  /**
   * Returns the size of the game board, which must be one of the valid sizes
   * defined in Utils.VALID_SIZES.
   *
   * @return the size of the game board.
   */
  public int getSize() {
    return size;
  }

  /** Returns whether verbose output is enabled, providing more detailed information
   * during the game.
   *
   * @return true if verbose output is enabled, false otherwise.
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Returns whether debug mode is enabled, which may include additional logging for
   * troubleshooting purposes.
   *
   * @return true if debug mode is enabled, false otherwise.
   */
  public boolean isDebug() {
    return debug;
  }

  /**
   * Returns whether the white player is controlled by AI.
   *
   * @return true if the white player is controlled by AI, false otherwise.
   */
  public boolean iswhiteAi() {
    return whiteAi;
  }

  /**
   * Returns whether the black player is controlled by AI.
   *
   * @return true if the black player is controlled by AI, false otherwise.
   */
  public boolean isblackAi() {
    return blackAi;
  }

  /**
   * Returns the time limit for AI moves in milliseconds.
   *
   * @return the time limit for AI moves in milliseconds.
   */
  public long getAiTime() {
    return aiTime;
  }

  /**
  * Returns the mode for the AI.
  *
  * @return the mode for the AI.
  */
  public String getAiMode() {
    return aiMode;
  }

  /**
  * Returns the search depth for the AI.
  *
  * @return the search depth for the AI.
  */
  public int getAiDepth() {
    return aiDepth;
  }

  /**
   * Returns a string representation of the Configuration object, including all
   * the settings and their current values. This method is useful for debugging
   * and logging purposes, allowing developers to easily see the configuration
   * state at any point in time.
   *
   * @return a string representation of the Configuration object.
   */
  @Override
  public String toString() {
    return "blitz=" + blitz + ", time=" + time + ", contest=" + contest
        + ", size=" + size + ", verbose=" + verbose + ", debug=" + debug
        + ", whiteAi=" + whiteAi + ", blackAi=" + blackAi + ", aiTime=" + aiTime + ", aiMode="
        + aiMode + ", aiDepth=" + aiDepth;
  }
}