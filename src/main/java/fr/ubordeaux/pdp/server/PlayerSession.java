package fr.ubordeaux.pdp.server;

import java.io.PrintWriter;

/**
 * Represents a player currently connected to the game server.
 *
 * <p>Holds the player's identity, current status, cumulative score statistics, and the
 * output stream used to push messages back to their client.
 *
 * <p>Thread-safety: score fields are updated only through {@code synchronized} methods.
 */
public class PlayerSession {

  /**
   * Possible lifecycle states for a connected player.
   *
   * <pre>
   * IDLE      → available; default state after connecting or finishing a game.
   * AWAY      → marked absent; invitations cannot be sent to this player.
   * WAITGAME  → has received an invitation and is waiting to accept or decline.
   * INGAME    → currently playing a game session.
   * </pre>
   */
  public enum Status {
    IDLE,
    AWAY,
    WAITGAME,
    INGAME
  }

  private final String id;
  private final String name;
  private final PrintWriter out;
  private final String interfaceMode;

  private volatile Status status = Status.IDLE;

  private int wins = 0;
  private int losses = 0;
  private int draws = 0;
  private int gamesPlayed = 0;

  /**
   * Creates a new player session.
   *
   * @param id unique player identifier chosen during registration.
   * @param name display name.
   * @param out output stream bound to the player's TCP socket.
   * @param interfaceMode client interface mode ({@code GUI} or {@code CLI}).
   */
  public PlayerSession(String id, String name, PrintWriter out, String interfaceMode) {
    this.id = id;
    this.name = name;
    this.out = out;
    this.interfaceMode = interfaceMode;
  }

  /**
   * Sends a text line to this player's client.
   *
   * @param message the line to send.
   */
  public void send(String message) {
    out.println(message);
  }

  /**
   * Returns the player's unique identifier.
   *
   * @return the player's unique identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the player's display name.
   *
   * @return the player's display name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the player's current status.
   *
   * @return the player's current status
   */
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

  /**
   * Returns whether the player is currently idle (available for a new game or invitation).
   *
   * <p>Only {@link Status#IDLE} players can receive invitations and be auto-matched.
   * {@link Status#AWAY} and {@link Status#WAITGAME} are both considered non-idle.
   *
   * @return {@code true} if the player's status is {@link Status#IDLE}.
   */
  public boolean isIdle() {
    return status == Status.IDLE;
  }

  /**
   * Returns whether the player has marked themselves as away.
   *
   * @return {@code true} if the player's status is {@link Status#AWAY}.
   */
  public boolean isAway() {
    return status == Status.AWAY;
  }

  /**
   * Returns whether the player is waiting for an invitation response.
   *
   * @return {@code true} if the player's status is {@link Status#WAITGAME}.
   */
  public boolean isWaitingForInvitation() {
    return status == Status.WAITGAME;
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

  /**
   * Records a draw and increments the games-played counter.
   *
   * <p>Must be called instead of {@link #recordLoss()} when a game ends with no winner,
   * so that draws are not incorrectly counted as losses.
   */
  public synchronized void recordDraw() {
    draws++;
    gamesPlayed++;
  }

  /**
   * Returns the total number of wins.
   *
   * @return the total number of wins
   */
  public int getWins() {
    return wins;
  }

  /**
   * Returns the total number of losses.
   *
   * @return the total number of losses
   */
  public int getLosses() {
    return losses;
  }

  /**
   * Returns the total number of draws.
   *
   * @return the total number of draws
   */
  public int getDraws() {
    return draws;
  }

  /**
   * Returns the total number of games played.
   *
   * @return the total number of games played
   */

  public int getGamesPlayed() {
    return gamesPlayed;
  }

  /**
   * Returns the player's client interface mode.
   *
   * @return the player's client interface mode ({@code GUI} or {@code CLI})
   */
  public String getInterfaceMode() {
    return interfaceMode;
  }

  @Override
  public String toString() {
    return String.format(
        "ID       : %s%n"
            + "Name     : %s%n"
            + "Status   : %s%n"
            + "Games    : %d%n"
            + "Wins     : %d%n"
            + "Losses   : %d%n"
            + "Draws    : %d",
        id, name, status.name().toLowerCase(),
        gamesPlayed, wins, losses, draws);
  }
}