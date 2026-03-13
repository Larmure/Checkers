
package fr.ubordeaux.pdp.model.core;

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
  private final boolean blitz;
  private final int time;
  private final boolean contest;
  private final int size;
  private final boolean verbose;
  private final boolean debug;
  private final boolean whiteAi;
  private final boolean blackAi;

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
   */
  public Configuration(boolean blitz, int time, boolean contest, int size,
      boolean verbose, boolean debug, boolean whiteAi, boolean blackAi) {
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

    this.blitz = blitz;
    this.time = time;
    this.contest = contest;
    this.size = size;
    this.verbose = verbose;
    this.debug = debug;
    this.whiteAi = whiteAi;
    this.blackAi = blackAi;
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
        Utils.DEFAULT_BLACK_AI);
  }

  public boolean isBlitz() {
    return blitz;
  }

  public int getTime() {
    return time;
  }

  public boolean isContest() {
    return contest;
  }

  public int getSize() {
    return size;
  }

  public boolean isVerbose() {
    return verbose;
  }

  public boolean isDebug() {
    return debug;
  }

  public boolean isblackAi() {
    return blackAi;
  }

  public boolean iswhiteAi() {
    return whiteAi;
  }

  @Override
  public String toString() {
    return "blitz=" + blitz + ", time=" + time + ", contest=" + contest
        + ", size=" + size + ", verbose=" + verbose + ", debug=" + debug
        + ", whiteAi=" + whiteAi + ", blackAi=" + blackAi;
  }
}