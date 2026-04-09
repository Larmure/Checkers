package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents one active game between two or more players on the server.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Tracks whose turn it is and enforces turn order (anti-cheat).
 *   <li>Validates that a {@code MOVE} comes from the correct player.
 *   <li>Routes {@code OPPONENT_MOVE} notifications to all other participants.
 *   <li>Delegates move legality to {@link GameController}.
 *   <li>Records wins, losses, and draws when the session ends.
 * </ul>
 *
 * <p>Thread-safety: {@link #handleMove} and {@link #end} are {@code synchronized} to
 * prevent race conditions from concurrent client threads.
 */
public class GameSession {

  private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);

  private final String sessionId;
  private final List<PlayerSession> players;
  private final GameController controller;
  private int currentTurnIndex = 0;
  private volatile boolean active = true;

  /**
   * Creates a new session and marks all participants as {@code INGAME}.
   *
   * @param players ordered list of participants; turn order follows list order.
   * @param controller the game controller that will validate and execute moves.
   */
  public GameSession(List<PlayerSession> players, GameController controller) {
    this.sessionId = "GAME-" + ID_COUNTER.getAndIncrement();
    this.players = Collections.unmodifiableList(new ArrayList<>(players));
    this.controller = controller;

    for (PlayerSession player : this.players) {
      player.setStatus(PlayerSession.Status.INGAME);
    }
  }

  /**
   * Processes a move from the given player.
   *
   * <p>Validation steps:
   *
   * <ol>
   *   <li>The session must be active.
   *   <li>It must be the sender's turn.
   *   <li>The move string must follow the {@code FROM-TO} format (e.g. {@code e2-e4}).
   *   <li>The move must be legal according to {@link GameController}.
   * </ol>
   *
   * <p>On success, broadcasts {@code OPPONENT_MOVE <move>} to all other players and
   * advances the turn index.
   *
   * @param sender the player attempting the move.
   * @param moveArgs the raw move string from the client (e.g. {@code "e2-e4"}).
   * @return an error message if the move was rejected, or {@code null} on success.
   */
  public synchronized String handleMove(PlayerSession sender, String moveArgs) {
    if (!active) {
      return Internationalization.get("server.game.session_inactive", sessionId);
    }

    PlayerSession expected = players.get(currentTurnIndex);
    if (!expected.getId().equals(sender.getId())) {
      return Internationalization.get("server.game.not_your_turn", expected.getId());
    }

    String[] parts = moveArgs.trim().split("-");
    if (parts.length != 2) {
      return Internationalization.get("server.game.invalid_move_format");
    }

    try {
      controller.executeMove(parts[0].trim(), parts[1].trim(), false);
    } catch (Exception e) {
      return Internationalization.get("server.game.illegal_move", e.getMessage());
    }

    String notification = Internationalization.get(
        "server.game.notification.opponent_move",
        moveArgs.trim());
    for (PlayerSession player : players) {
      if (!player.getId().equals(sender.getId())) {
        player.send(notification);
      }
    }

    currentTurnIndex = (currentTurnIndex + 1) % players.size();
    return null;
  }

  /**
   * Ends the session, records scores, notifies all players, and returns them to {@code IDLE}.
   *
   * <p>Score rules:
   * <ul>
   *   <li>If {@code winnerId} is non-null, the matching player gets a win; all others get a loss.
   *   <li>If {@code winnerId} is {@code null} (draw), every player gets a draw — NOT a loss.
   * </ul>
   *
   * @param winnerId the ID of the winning player, or {@code null} for a draw.
   */
  public synchronized void end(String winnerId) {
    if (!active) {
      return;
    }
    active = false;

    boolean isDraw = (winnerId == null);

    for (PlayerSession player : players) {
      if (isDraw) {
        player.recordDraw();
      } else if (player.getId().equals(winnerId)) {
        player.recordWin();
      } else {
        player.recordLoss();
      }
      player.setStatus(PlayerSession.Status.IDLE);
      player.send(isDraw
          ? Internationalization.get("server.game.notification.game_over_draw")
          : Internationalization.get("server.game.notification.game_over_winner", winnerId));
    }
  }

  /**
   * Returns the unique session identifier.
   *
   * @return the unique session identifier
   */
  public String getSessionId() {
    return sessionId;
  }

  /**
   * Returns the participants in turn order.
   *
   * @return an unmodifiable view of the participants in turn order
   */
  public List<PlayerSession> getPlayers() {
    return players;
  }

  /**
   * Returns whether the session is still active.
   *
   * @return {@code true} if the session has not yet ended
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Returns the player whose turn it currently is.
   *
   * @return the player whose turn it currently is
   */
  public PlayerSession getCurrentPlayer() {
    return players.get(currentTurnIndex);
  }

  /**
   * Returns whether a player with the given ID is part of this session.
   *
   * @param playerId the player ID to check.
   * @return {@code true} if the player is a participant.
   */
  public boolean hasPlayer(String playerId) {
    return players.stream().anyMatch(p -> p.getId().equals(playerId));
  }

  @Override
  public String toString() {
    StringBuilder playersText = new StringBuilder();
    for (int i = 0; i < players.size(); i++) {
      if (i > 0) {
        playersText.append(Internationalization.get("server.game.players_separator"));
      }
      playersText.append(players.get(i).getId());
    }
    return Internationalization.get(
        "server.game.format",
        sessionId,
        playersText.toString(),
        getCurrentPlayer().getId());
  }
}