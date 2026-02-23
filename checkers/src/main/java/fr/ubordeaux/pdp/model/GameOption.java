
package fr.ubordeaux.pdp.model;

public class GameOption {
  private final boolean blitz;
  private final int time;
  private final boolean contest;
  private final int size;

  public GameOption(boolean blitz, int time, boolean contest, int size) {
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

  @Override
  public String toString() {
    return "blitz=" + blitz + ", time=" + time + ", contest=" + contest + ", size=" + size;
  }
}