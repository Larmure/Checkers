
package fr.ubordeaux.pdp.model;

public class Configuration {
  private final boolean blitz;
  private final int time;
  private final boolean contest;
  private final int size;
  private final boolean verbose;
  private final boolean debug;

  public Configuration(boolean blitz, int time, boolean contest, int size, boolean verbose, boolean debug) {
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
  }

  public Configuration(Configuration other) {
    this.blitz = other.blitz;
    this.time = other.time;
    this.contest = other.contest;
    this.size = other.size;
    this.verbose = other.verbose;
    this.debug = other.debug;
  }

  public Configuration(Configuration other, boolean verbose, boolean debug) {
    this.blitz = other.blitz;
    this.time = other.time;
    this.contest = other.contest;
    this.size = other.size;
    this.verbose = verbose;
    this.debug = debug;
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

  @Override
  public String toString() {
    return "blitz=" + blitz + ", time=" + time + ", contest=" + contest + ", size=" + size + ", verbose=" + verbose + ", debug=" + debug;
  }
}