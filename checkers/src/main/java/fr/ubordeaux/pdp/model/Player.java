package fr.ubordeaux.pdp.model;

/**
 * Abstract representation of a player in the Checkers game.
 * Base class for HumanPlayer and IAPlayer as defined in the system architecture.
 */
public abstract class Player {

  private String name;
  private int playTime;

  /**
   * Returns the player's name.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the remaining play time (specifically used for Blitz mode).
   */
  public int getPlayTime() {
    return playTime;
  }

  /**
   * Sets the player's name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Updates the player's remaining play time.
   * Required for Blitz mode time tracking (F5, F14).
   */
  public void setPlayTime(int playTime) {
    this.playTime = playTime;
  }
}