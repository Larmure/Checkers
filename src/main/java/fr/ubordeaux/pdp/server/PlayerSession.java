package fr.ubordeaux.pdp.server;

import java.io.PrintWriter;

/**
 * Represents a player connected to the game server.
 *
 * <p>Holds the player's identity, current status, score statistics, and the
 * output stream used to send messages back to the client.
 *
 * <p>Thread safety: status and score fields are updated under synchronization
 * in GameRegistry.
 */
public class PlayerSession {

  /** Possible states for a connected player. */
  public enum Status {
    /** Player is idle and not in a game. */
    IDLE,
    /** Player is currently in a game. */
    INGAME
  }

  /** The unique player identifier. */
  private final String id;
  /** The player name. */
  private final String name;
  /** The output stream used to send messages to the client. */
  private final PrintWriter out;

  /** The current player status. */
  private volatile Status status = Status.IDLE;

  /** The number of games won by the player. */
  private int wins = 0;
  /** The number of games lost by the player. */
  private int losses = 0;
  /** The total number of games played by the player. */
  private int gamesPlayed = 0;

  /**
   * Creates a player session.
   *
   * @param id the unique player identifier
   * @param name the player name
   * @param out the output stream used to send messages to the client
   */
  public PlayerSession(String id, String name, PrintWriter out) {
    this.id = id;
    this.name = name;
    this.out = out;
  }

  /**
   * Sends a message to the client associated with this player session.
   *
   * @param message the message to send to the client
   */
  public void send(String message) {
    out.println(message);
  }

  /**
   * Returns the current status of the player (e.g., idle, in-game).
   *
   * @return the player's current status
   */
  public Status getStatus() {
    return status;
  }

  /** Sets the player's status to the specified value.
   *
   * @param status the new status for the player
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /** Returns whether the player is currently idle. 
   *
   * @return true if the player is idle, false if they are in a game
   */
  public boolean isIdle() {
    return status == Status.IDLE;
  }

  /** Records a win for this player. */
  public synchronized void recordWin() {
    wins++;
    gamesPlayed++;
  }

  /** Records a loss for this player. */
  public synchronized void recordLoss() {
    losses++;
    gamesPlayed++;
  }

  /**
   * Returns the number of wins for this player.
   *
   * @return the total wins
   */
  public int getWins() {
    return wins;
  }

  /**
   * Returns the number of losses for this player.
   *
   * @return the total losses
   */
  public int getLosses() {
    return losses;
  }

  /**
   * Returns the number of games played by this player.
   *
   * @return the total games played
   */
  public int getGamesPlayed() {
    return gamesPlayed;
  }

  /**
   * Returns the unique identifier for this player session.
   *
   * @return the player's unique ID
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the name of the player associated with this session.
   *
   * @return the player's name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns a string representation of the player session, including the player's ID, name,
   * status, and score statistics.
   */
  @Override
  public String toString() {
    return String.format(
        "%-10s %-15s %-6s W:%d L:%d",
        id,
        name,
        status.name().toLowerCase(),
        wins,
        losses);
  }
}