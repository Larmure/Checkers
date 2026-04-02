package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe registry of connected players and active game sessions.
 *
 * <p>All structural mutations (register/remove player, create/end session) are
 * {@code synchronized} to prevent race conditions from concurrent client threads.
 * Read-only accessors use the underlying {@link ConcurrentHashMap} directly and do not
 * require additional locking.
 *
 * <p>This class is the single source of truth for:
 *
 * <ul>
 *   <li>Which players are connected and what their current status is.
 *   <li>Which game sessions are active and which players are in them.
 * </ul>
 */
public class GameRegistry {

  private final Map<String, PlayerSession> players = new ConcurrentHashMap<>();
  private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

  /**
   * Registers a new player. Returns {@code null} if the ID is already taken.
   *
   * @param id unique identifier chosen by the client.
   * @param name display name.
   * @param out output stream bound to the player's TCP socket.
   * @param interfaceMode client interface mode ({@code GUI} or {@code CLI}).
   * @return the created {@link PlayerSession}, or {@code null} if the ID is in use.
   */
  public synchronized PlayerSession registerPlayer(
      String id, String name, PrintWriter out, String interfaceMode) {
    if (players.containsKey(id)) {
      return null;
    }
    PlayerSession player = new PlayerSession(id, name, out, interfaceMode);
    players.put(id, player);
    return player;
  }

  /**
   * Removes a player and ends any game session they were part of.
   *
   * @param playerId the ID of the player to remove.
   */
  public synchronized void removePlayer(String playerId) {
    players.remove(playerId);

    sessions.values().removeIf(
        session -> {
          if (session.hasPlayer(playerId) && session.isActive()) {
            session.end(null);
            return true;
          }
          return !session.isActive();
        });
  }

  /**
   * Looks up a player by ID.
   *
   * @param playerId the player's unique ID.
   * @return the {@link PlayerSession}, or {@code null} if not found.
   */
  public PlayerSession getPlayer(String playerId) {
    return players.get(playerId);
  }

  /**
   * Returns a snapshot of all currently connected players.
   *
   * @return an unmodifiable collection of player sessions.
   */
  public Collection<PlayerSession> getAllPlayers() {
    return List.copyOf(players.values());
  }

  /**
   * Returns the number of connected players.
   *
   * @return the number of connected players
   */
  public int getPlayerCount() {
    return players.size();
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
    return players.values().stream()
        .map(PlayerSession::toString)
        .collect(Collectors.joining("\n"));
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
    if (players.isEmpty()) {
      return "No players connected.";
    }
    return players.values().stream()
        .sorted(Comparator.comparingInt(PlayerSession::getWins).reversed())
        .map(PlayerSession::toString)
        .collect(Collectors.joining("\n"));
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
  public synchronized GameSession createSession(
      List<PlayerSession> participants, GameController controller) {
    GameSession session = new GameSession(participants, controller);
    sessions.put(session.getSessionId(), session);
    return session;
  }

  /**
   * Finds the active game session that the given player is currently in.
   *
   * @param playerId the player's ID.
   * @return the active {@link GameSession}, or {@code null} if not in any.
   */
  public GameSession getSessionForPlayer(String playerId) {
    return sessions.values().stream()
        .filter(s -> s.isActive() && s.hasPlayer(playerId))
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns all currently active game sessions.
   *
   * @return an unmodifiable list of active sessions.
   */
  public Collection<GameSession> getActiveSessions() {
    return sessions.values().stream().filter(GameSession::isActive).toList();
  }

  /**
   * Returns the number of currently active game sessions.
   *
   * @return the number of currently active game sessions
   */
  public int getActiveSessionCount() {
    return (int) sessions.values().stream().filter(GameSession::isActive).count();
  }

  /** Removes all sessions that have already ended. */
  public synchronized void cleanupEndedSessions() {
    sessions.values().removeIf(s -> !s.isActive());
  }
}