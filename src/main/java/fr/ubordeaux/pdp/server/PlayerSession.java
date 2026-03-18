package fr.ubordeaux.pdp.server;

import java.io.PrintWriter;

/**
 * Represents a player currently connected to the game server.
 *
 * <p>Holds the player's identity, current status, cumulative score statistics, and the
 * output stream used to push messages back to their client.
 *
 * <p>Thread-safety: status and score fields are updated only through synchronized methods
 * or under synchronization in {@link GameRegistry}.
 */
public class PlayerSession {

  /** Possible lifecycle states for a connected player. */
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
   * @param id unique player identifier chosen during registration.
   * @param name display name.
   * @param out output stream bound to the player's TCP socket.
   */
  public PlayerSession(String id, String name, PrintWriter out) {
    this.id = id;
    this.name = name;
    this.out = out;
  }

  /**
   * Sends a text line to this player's client.
   *
   * @param message the line to send.
   */
  public void send(String message) {
    out.println(message);
  }

  /** @return the player's unique identifier. */
  public String getId() {
    return id;
  }

  /** @return the player's display name. */
  public String getName() {
    return name;
  }

  /** @return the player's current status. */
  public Status getStatus() {
    return status;
  }

  /**
   * Updates the player's status.
   *
   * @param status the new status.
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /** @return {@code true} if the player is not currently in a game. */
  public boolean isIdle() {
    return status == Status.IDLE;
  }

  /** Records a win and increments the games-played counter. */
  public synchronized void recordWin() {
    wins++;
    gamesPlayed++;
  }

  /** Records a loss and increments the games-played counter. */
  public synchronized void recordLoss() {
    losses++;
    gamesPlayed++;
  }

  /** @return total wins. */
  public int getWins() {
    return wins;
  }

  /** @return total losses. */
  public int getLosses() {
    return losses;
  }

  /** @return total games played. */
  public int getGamesPlayed() {
    return gamesPlayed;
  }

  @Override
  public String toString() {
    return String.format("%-10s %-15s %-6s W:%d L:%d",
          id, name, status.name().toLowerCase(), wins, losses);
  }
}