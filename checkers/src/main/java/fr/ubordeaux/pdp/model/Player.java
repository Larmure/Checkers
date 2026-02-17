package fr.ubordeaux.pdp.model;

/**
 * Abstract representation of a participant in the Checkers game.
 * <p>Serves as the base class for both human-controlled players and AI agents.
 */
public abstract class Player {

  private String name;
  private int playTime;

  /**
   * Base constructor for a player.
   *
   * @param name The display name of the player.
   */
  public Player(String name) {
    this.name = name;
  }

  /**
   * Returns the player's identifier.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the remaining time allocated for this player.
   * <p>This is specifically used in timed modes (e.g., Blitz).
   */
  public int getPlayTime() {
    return playTime;
  }

  /**
   * Updates the player's display name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Updates the player's remaining play time.
   *
   * @param playTime The time in seconds (or ticks) remaining.
   */
  public void setPlayTime(int playTime) {
    this.playTime = playTime;
  }

  @Override
  public String toString() {
    return this.name;
  }
}