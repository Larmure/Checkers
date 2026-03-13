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
    IDLE,
    INGAME
  }

  private final String id;
  private final String name;
  private final PrintWriter out;

  private volatile Status status = Status.IDLE;

  private int wins = 0;
  private int losses = 0;
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

  /** Sends a message to this player's client. */
  public void send(String message) {
    out.println(message);
  }

  /** Returns the current player status. */
  public Status getStatus() {
    return status;
  }

  /** Updates the current player status. */
  public void setStatus(Status status) {
    this.status = status;
  }

  /** Returns whether the player is currently idle. */
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

  /** Returns the number of wins. */
  public int getWins() {
    return wins;
  }

  /** Returns the number of losses. */
  public int getLosses() {
    return losses;
  }

  /** Returns the number of games played. */
  public int getGamesPlayed() {
    return gamesPlayed;
  }

  /** Returns the unique player identifier. */
  public String getId() {
    return id;
  }

  /** Returns the player name. */
  public String getName() {
    return name;
  }

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