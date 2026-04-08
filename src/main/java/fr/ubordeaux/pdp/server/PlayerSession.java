package fr.ubordeaux.pdp.server;

import java.io.PrintWriter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a player currently connected to the game server.
 *
 * <p>Holds the player's identity, current status, cumulative score statistics, and the
 * output stream used to push messages back to their client.
 *
 * <h2>Thread-safety</h2>
 *
 * <p>A single {@link ReentrantLock} ({@code playerLock}) guards both the mutable
 * {@code status} field and all score counters. This is necessary because callers in
 * {@code GameServer} commonly perform check-then-act sequences such as:
 *
 * <pre>{@code
 * if (player.getStatus() == Status.INGAME) { ... }
 * else { player.setStatus(Status.AWAY); }
 * }</pre>
 *
 * <p>Protecting individual reads and writes with separate {@code synchronized} methods
 * would leave such compound sequences open to race conditions. Exposing the lock via
 * {@link #lock()} / {@link #unlock()} lets callers make the whole compound operation
 * atomic when needed.
 *
 * <p>The {@code out} field is final and {@link PrintWriter} is thread-safe for
 * individual {@code println} calls, so {@link #send(String)} requires no additional
 * locking.
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

  /**
   * Guards {@code status} and all score fields.
   *
   * <p>Exposed to callers that need to make compound read-modify-write sequences
   * on this player's state atomic (e.g. check status then change it).
   */
  private final ReentrantLock playerLock = new ReentrantLock();


  private final String id;
  private final String name;
  private final PrintWriter out;
  private Status status = Status.IDLE;
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
   */
  public PlayerSession(String id, String name, PrintWriter out) {
    this.id = id;
    this.name = name;
    this.out = out;
  }

  /**
   * Acquires the player's intrinsic lock.
   *
   * <p>Use this when a caller needs to make a multi-step operation atomic, e.g.:
   *
   * <pre>{@code
   * player.lock();
   * try {
   *   if (player.getStatus() == Status.IDLE) {
   *     player.setStatus(Status.AWAY);
   *   }
   * } finally {
   *   player.unlock();
   * }
   * }</pre>
   */
  public void lock() {
    playerLock.lock();
  }

  /** Releases the player's intrinsic lock. Always call in a {@code finally} block. */
  public void unlock() {
    playerLock.unlock();
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
    playerLock.lock();
    try {
      return status;
    } finally {
      playerLock.unlock();
    }
  }

  /**
   * Updates the player's status.
   *
   * @param status the new status.
   */
  public void setStatus(Status status) {
    playerLock.lock();
    try {
      this.status = status;
    } finally {
      playerLock.unlock();
    }
  }

  /**
   * Returns whether the player is currently idle (available for a new game or invitation).
   *
   * <p>Only {@link Status#IDLE} players can receive invitations and be auto-matched.
   *
   * @return {@code true} if the player's status is {@link Status#IDLE}.
   */
  public boolean isIdle() {
    playerLock.lock();
    try {
      return status == Status.IDLE;
    } finally {
      playerLock.unlock();
    }
  }

  /**
   * Returns whether the player has marked themselves as away.
   *
   * @return {@code true} if the player's status is {@link Status#AWAY}.
   */
  public boolean isAway() {
    playerLock.lock();
    try {
      return status == Status.AWAY;
    } finally {
      playerLock.unlock();
    }
  }

  /**
   * Returns whether the player is waiting for an invitation response.
   *
   * @return {@code true} if the player's status is {@link Status#WAITGAME}.
   */
  public boolean isWaitingForInvitation() {
    playerLock.lock();
    try {
      return status == Status.WAITGAME;
    } finally {
      playerLock.unlock();
    }
  }

  /** Records a win and increments the games-played counter. */
  public void recordWin() {
    playerLock.lock();
    try {
      wins++;
      gamesPlayed++;
    } finally {
      playerLock.unlock();
    }
  }

  /** Records a loss and increments the games-played counter. */
  public void recordLoss() {
    playerLock.lock();
    try {
      losses++;
      gamesPlayed++;
    } finally {
      playerLock.unlock();
    }
  }

  /**
   * Records a draw and increments the games-played counter.
   *
   * <p>Must be called instead of {@link #recordLoss()} when a game ends with no winner,
   * so that draws are not incorrectly counted as losses.
   */
  public void recordDraw() {
    playerLock.lock();
    try {
      draws++;
      gamesPlayed++;
    } finally {
      playerLock.unlock();
    }
  }

  /**
   * Returns the total number of wins.
   *
   * @return the total number of wins
   */
  public int getWins() {
    playerLock.lock();
    try {
      return wins;
    } finally {
      playerLock.unlock();
    }
  }

  /**
   * Returns the total number of losses.
   *
   * @return the total number of losses
   */
  public int getLosses() {
    playerLock.lock();
    try {
      return losses;
    } finally {
      playerLock.unlock();
    }
  }

  /**
   * Returns the total number of draws.
   *
   * @return the total number of draws
   */
  public int getDraws() {
    playerLock.lock();
    try {
      return draws;
    } finally {
      playerLock.unlock();
    }
  }

  /**
   * Returns the total number of games played.
   *
   * @return the total number of games played
   */
  public int getGamesPlayed() {
    playerLock.lock();
    try {
      return gamesPlayed;
    } finally {
      playerLock.unlock();
    }
  }

  /**
   * Sends a text line to this player's client.
   *
   * <p>{@link PrintWriter#println(String)} is internally synchronized;
   * no additional locking is needed here.
   *
   * @param message the line to send.
   */
  public void send(String message) {
    out.println(message);
  }


  @Override
  public String toString() {
    playerLock.lock();
    try {
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
    } finally {
      playerLock.unlock();
    }
  }
}