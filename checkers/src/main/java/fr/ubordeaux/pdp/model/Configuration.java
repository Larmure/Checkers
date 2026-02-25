
package fr.ubordeaux.pdp.model;

public class Configuration {
  private final boolean blitz;
  private final int time;
  private final boolean contest;
  private final int size;
  private final boolean verbose;
  private final boolean debug;
  private final boolean whiteIsAI;
  private final boolean blackIsAI;

  public Configuration(boolean blitz, int time, boolean contest, int size, boolean verbose, boolean debug, boolean whiteIsAI, boolean blackIsAi) {
    if (!blitz && time != Utils.DEFAULT_TIME) {
      System.out.println("Warning: time option used without blitz option.");
      blitz = Utils.DEFAULT_BLITZ;
      time = Utils.DEFAULT_TIME;
    }
    if (!Utils.VALID_SIZES.contains(size)) {
      System.out.println("Warning: Invalid board size, changed to " + Utils.DEFAULT_BOARD_SIZE + ".");
      size = Utils.DEFAULT_BOARD_SIZE;
    }

    this.blitz = blitz;
    this.time = time;
    this.contest = contest;
    this.size = size;
    this.verbose = verbose;
    this.debug = debug;
    this.whiteIsAI = whiteIsAI;
    this.blackIsAI = blackIsAi;
  }

  public Configuration(Configuration other) {
    this.blitz = other.blitz;
    this.time = other.time;
    this.contest = other.contest;
    this.size = other.size;
    this.verbose = other.verbose;
    this.debug = other.debug;
    this.whiteIsAI = other.whiteIsAI;
    this.blackIsAI = other.blackIsAI;
  }

  public Configuration(Configuration other, boolean verbose, boolean debug) {
    this.blitz = other.blitz;
    this.time = other.time;
    this.contest = other.contest;
    this.size = other.size;
    this.verbose = verbose;
    this.debug = debug;
    this.whiteIsAI = other.whiteIsAI;
    this.blackIsAI = other.blackIsAI;
  }

  public static Configuration getDefaultConfiguration() {
    return new Configuration(Utils.DEFAULT_BLITZ, Utils.DEFAULT_TIME, Utils.DEFAULT_CONTEST, Utils.DEFAULT_BOARD_SIZE, Utils.DEFAULT_VERBOSE, Utils.DEFAULT_DEBUG, Utils.DEFAULT_WHITE_AI, Utils.DEFAULT_BLACK_AI);
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

  public boolean isBlackIsAI() {
    return blackIsAI;
  }

  public boolean isWhiteIsAI() {
    return whiteIsAI;
  }

  @Override
  public String toString() {
    return "blitz=" + blitz + ", time=" + time + ", contest=" + contest + ", size=" + size + ", verbose=" 
      + verbose + ", debug=" + debug + ", whiteAI=" + whiteIsAI + ", blackAI=" + blackIsAI;
  }
}