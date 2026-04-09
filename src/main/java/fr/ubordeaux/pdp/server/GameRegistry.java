package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Thread-safe registry of connected players and active game sessions.
 *
 * <h2>Locking strategy</h2>
 *
 * <p>A {@link ReentrantReadWriteLock} replaces the previous coarse-grained
 * {@code synchronized} approach:
 *
 * <ul>
 *   <li><b>Write lock</b> — acquired for all structural mutations:
 *       {@link #registerPlayer}, {@link #removePlayer}, {@link #createSession},
 *       and {@link #cleanupEndedSessions}.
 *   <li><b>Read lock</b> — acquired for all read-only accessors:
 *       {@link #getPlayer}, {@link #getAllPlayers}, {@link #getPlayerCount},
 *       {@link #getPlayersFormatted}, {@link #getScoreboardFormatted},
 *       {@link #getSessionForPlayer}, {@link #getActiveSessions}, and
 *       {@link #getActiveSessionCount}.
 * </ul>
 *
 * <p>This allows multiple reader threads (e.g. concurrent client handlers calling
 * {@code PLAYERS} or {@code SCOREBOARD}) to proceed in parallel while writers
 * still get exclusive access.
 *
 * <p>Both maps remain {@link ConcurrentHashMap} instances so that the
 * {@link java.util.concurrent.ConcurrentHashMap#size()} and
 * {@link java.util.concurrent.ConcurrentHashMap#entrySet()} operations
 * are cheap and non-blocking within the lock scopes.
 */
public class GameRegistry {

  /**
   * Fair ReadWriteLock so that long-running reader threads cannot starve writers.
   *
   * <p>Fairness adds a small overhead per acquisition but prevents the starvation
   * scenario where a flood of {@code PLAYERS} commands blocks a
   * {@code REGISTER} from completing.
   */
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock(/* fair= */ true);
  private final Map<String, PlayerSession> players = new ConcurrentHashMap<>();
  private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

  /**
   * Registers a new player. Returns {@code null} if the ID is already taken.
   *
   * @param id unique identifier chosen by the client.
   * @param name display name.
   * @param out output stream bound to the player's TCP socket.
   * @return the created {@link PlayerSession}, or {@code null} if the ID is in use.
   */
  public PlayerSession registerPlayer(String id, String name, PrintWriter out) {
    rwLock.writeLock().lock();
    try {
      if (players.containsKey(id)) {
        return null;
      }
      PlayerSession player = new PlayerSession(id, name, out);
      players.put(id, player);
      return player;
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  /**
   * Removes a player and ends any game session they were part of.
   *
   * @param playerId the ID of the player to remove.
   */
  public void removePlayer(String playerId) {
    rwLock.writeLock().lock();
    try {
      players.remove(playerId);

      sessions.values().removeIf(session -> {
        if (session.hasPlayer(playerId) && session.isActive()) {
          session.end(null);
          return true;
        }
        return !session.isActive();
      });
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  /**
   * Creates a new game session for the given participants.
   *
   * <p>Pre-condition: all players must be in {@code IDLE} status. Enforced by the caller.
   *
   * @param participants ordered list of players; turn order follows list order.
   * @param controller the game controller that will handle move execution.
   * @return the newly created {@link GameSession}.
   */
  public GameSession createSession(List<PlayerSession> participants, GameController controller) {
    rwLock.writeLock().lock();
    try {
      GameSession session = new GameSession(participants, controller);
      sessions.put(session.getSessionId(), session);
      return session;
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  /** Removes all sessions that have already ended. */
  public void cleanupEndedSessions() {
    rwLock.writeLock().lock();
    try {
      sessions.values().removeIf(s -> !s.isActive());
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  /**
   * Looks up a player by ID.
   *
   * @param playerId the player's unique ID.
   * @return the {@link PlayerSession}, or {@code null} if not found.
   */
  public PlayerSession getPlayer(String playerId) {
    rwLock.readLock().lock();
    try {
      return players.get(playerId);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Returns a snapshot of all currently connected players.
   *
   * @return an unmodifiable collection of player sessions.
   */
  public Collection<PlayerSession> getAllPlayers() {
    rwLock.readLock().lock();
    try {
      return List.copyOf(players.values());
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Returns the number of connected players.
   *
   * @return the number of connected players
   */
  public int getPlayerCount() {
    rwLock.readLock().lock();
    try {
      return players.size();
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Returns a formatted multi-line string listing all connected players.
   *
   * <p>Each line follows the format produced by {@link PlayerSession#toString()}.
   * Returns an empty string if no players are connected.
   *
   * @return formatted player list, ready to send to a client.
   */
  public String getPlayersFormatted() {
    rwLock.readLock().lock();
    try {
      return players.values().stream()
          .map(PlayerSession::toString)
          .collect(Collectors.joining("\n"));
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Returns a formatted scoreboard sorted by wins descending.
   *
   * <p>Each line follows the format produced by {@link PlayerSession#toString()}.
   * Returns a placeholder string if no players are connected.
   *
   * @return formatted scoreboard, ready to send to a client.
   */
  public String getScoreboardFormatted() {
    rwLock.readLock().lock();
    try {
      if (players.isEmpty()) {
        return "No players connected.";
      }
      return players.values().stream()
          .sorted(Comparator.comparingInt(PlayerSession::getWins).reversed())
          .map(PlayerSession::toString)
          .collect(Collectors.joining("\n"));
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Finds the active game session that the given player is currently in.
   *
   * @param playerId the player's ID.
   * @return the active {@link GameSession}, or {@code null} if not in any.
   */
  public GameSession getSessionForPlayer(String playerId) {
    rwLock.readLock().lock();
    try {
      return sessions.values().stream()
          .filter(s -> s.isActive() && s.hasPlayer(playerId))
          .findFirst()
          .orElse(null);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Returns all currently active game sessions.
   *
   * @return an unmodifiable list of active sessions.
   */
  public Collection<GameSession> getActiveSessions() {
    rwLock.readLock().lock();
    try {
      return sessions.values().stream().filter(GameSession::isActive).toList();
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /**
   * Returns the number of currently active game sessions.
   *
   * @return the number of currently active game sessions
   */
  public int getActiveSessionCount() {
    rwLock.readLock().lock();
    try {
      return (int) sessions.values().stream().filter(GameSession::isActive).count();
    } finally {
      rwLock.readLock().unlock();
    }
  }
}