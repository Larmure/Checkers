package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.view.HeadlessView;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GameRegistry}.
 */
class GameRegistryTest {

  @Test
  void registerPlayer_addsPlayerWhenIdIsFree() {
    GameRegistry registry = new GameRegistry();
    PrintWriter out = new PrintWriter(new StringWriter(), true);

    PlayerSession player = registry.registerPlayer("p1", "Daniel", out);

    assertNotNull(player);
    assertEquals("p1", player.getId());
    assertEquals("Daniel", player.getName());
    assertEquals(1, registry.getPlayerCount());
    assertEquals(player, registry.getPlayer("p1"));
  }

  @Test
  void registerPlayer_returnsNullWhenIdAlreadyExists() {
    GameRegistry registry = new GameRegistry();
    PrintWriter out1 = new PrintWriter(new StringWriter(), true);
    PrintWriter out2 = new PrintWriter(new StringWriter(), true);

    PlayerSession first = registry.registerPlayer("p1", "Daniel", out1);
    PlayerSession second = registry.registerPlayer("p1", "Other", out2);

    assertNotNull(first);
    assertNull(second);
    assertEquals(1, registry.getPlayerCount());
    assertEquals("Daniel", registry.getPlayer("p1").getName());
  }

  @Test
  void getPlayer_returnsNullWhenUnknown() {
    GameRegistry registry = new GameRegistry();

    assertNull(registry.getPlayer("unknown"));
  }

  @Test
  void getAllPlayers_returnsSnapshotOfRegisteredPlayers() {
    GameRegistry registry = new GameRegistry();
    registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));
    registry.registerPlayer("p2", "Alice", new PrintWriter(new StringWriter(), true));

    Collection<PlayerSession> players = registry.getAllPlayers();

    assertEquals(2, players.size());
    assertTrue(players.stream().anyMatch(p -> p.getId().equals("p1")));
    assertTrue(players.stream().anyMatch(p -> p.getId().equals("p2")));
  }

  @Test
  void getPlayersFormatted_returnsEmptyStringWhenNoPlayers() {
    GameRegistry registry = new GameRegistry();

    assertEquals("", registry.getPlayersFormatted());
  }

  @Test
  void getPlayersFormatted_containsRegisteredPlayers() {
    GameRegistry registry = new GameRegistry();
    registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));
    registry.registerPlayer("p2", "Alice", new PrintWriter(new StringWriter(), true));

    String formatted = registry.getPlayersFormatted();

    assertTrue(formatted.contains("p1"));
    assertTrue(formatted.contains("Daniel"));
    assertTrue(formatted.contains("p2"));
    assertTrue(formatted.contains("Alice"));
  }

  @Test
  void getScoreboardFormatted_returnsPlaceholderWhenNoPlayers() {
    GameRegistry registry = new GameRegistry();

    assertEquals(
        Internationalization.get("server.registry.no_players_connected"),
        registry.getScoreboardFormatted());
  }

  @Test
  void getScoreboardFormatted_sortsByWinsDescending() {
    GameRegistry registry = new GameRegistry();

    PlayerSession p1 =
        registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));
    PlayerSession p2 =
        registry.registerPlayer("p2", "Alice", new PrintWriter(new StringWriter(), true));
    PlayerSession p3 =
        registry.registerPlayer("p3", "Bob", new PrintWriter(new StringWriter(), true));

    p1.recordWin();
    p1.recordWin();
    p2.recordWin();

    String scoreboard = registry.getScoreboardFormatted();

    int idxP1 = scoreboard.indexOf(String.format(
        Internationalization.get("server.player.line.id"), "p1"));
    int idxP2 = scoreboard.indexOf(String.format(
        Internationalization.get("server.player.line.id"), "p2"));
    int idxP3 = scoreboard.indexOf(String.format(
        Internationalization.get("server.player.line.id"), "p3"));

    assertTrue(idxP1 >= 0);
    assertTrue(idxP2 >= 0);
    assertTrue(idxP3 >= 0);
    assertTrue(idxP1 < idxP2, "p1 doit apparaître avant p2");
    assertTrue(idxP2 < idxP3, "p2 doit apparaître avant p3");
  }

  @Test
  void removePlayer_removesOnlyPlayerWhenNoSessionExists() {
    GameRegistry registry = new GameRegistry();
    registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));

    registry.removePlayer("p1");

    assertEquals(0, registry.getPlayerCount());
    assertNull(registry.getPlayer("p1"));
  }

  @Test
  void createSession_createsActiveSession() {
    GameRegistry registry = new GameRegistry();
    PlayerSession p1 =
        registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));
    PlayerSession p2 =
        registry.registerPlayer("p2", "Alice", new PrintWriter(new StringWriter(), true));
    GameController controller = new DummyGameController();

    GameSession session = registry.createSession(List.of(p1, p2), controller);

    assertNotNull(session);
    assertTrue(session.isActive());
    assertEquals(1, registry.getActiveSessionCount());
    assertTrue(registry.getActiveSessions().contains(session));
  }

  @Test
  void getSessionForPlayer_returnsSessionForParticipant() {
    GameRegistry registry = new GameRegistry();
    PlayerSession p1 =
        registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));
    PlayerSession p2 =
        registry.registerPlayer("p2", "Alice", new PrintWriter(new StringWriter(), true));
    GameController controller = new DummyGameController();

    GameSession session = registry.createSession(List.of(p1, p2), controller);

    assertEquals(session, registry.getSessionForPlayer("p1"));
    assertEquals(session, registry.getSessionForPlayer("p2"));
  }

  @Test
  void getSessionForPlayer_returnsNullWhenPlayerIsNotInSession() {
    GameRegistry registry = new GameRegistry();
    registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));

    assertNull(registry.getSessionForPlayer("p1"));
    assertNull(registry.getSessionForPlayer("unknown"));
  }

  @Test
  void getActiveSessions_returnsOnlyActiveSessions() {
    GameRegistry registry = new GameRegistry();
    PlayerSession p1 =
        registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));
    PlayerSession p2 =
        registry.registerPlayer("p2", "Alice", new PrintWriter(new StringWriter(), true));
    PlayerSession p3 =
        registry.registerPlayer("p3", "Bob", new PrintWriter(new StringWriter(), true));
    PlayerSession p4 =
        registry.registerPlayer("p4", "Eve", new PrintWriter(new StringWriter(), true));

    GameController controller = new DummyGameController();

    GameSession active = registry.createSession(List.of(p1, p2), controller);
    GameSession ended = registry.createSession(List.of(p3, p4), controller);
    ended.end(null);

    Collection<GameSession> activeSessions = registry.getActiveSessions();

    assertEquals(1, activeSessions.size());
    assertTrue(activeSessions.contains(active));
    assertFalse(activeSessions.contains(ended));
  }

  @Test
  void getActiveSessionCount_countsOnlyActiveSessions() {
    GameRegistry registry = new GameRegistry();
    PlayerSession p1 =
        registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));
    PlayerSession p2 =
        registry.registerPlayer("p2", "Alice", new PrintWriter(new StringWriter(), true));
    PlayerSession p3 =
        registry.registerPlayer("p3", "Bob", new PrintWriter(new StringWriter(), true));
    PlayerSession p4 =
        registry.registerPlayer("p4", "Eve", new PrintWriter(new StringWriter(), true));

    GameController controller = new DummyGameController();

    registry.createSession(List.of(p1, p2), controller);
    GameSession ended = registry.createSession(List.of(p3, p4), controller);
    ended.end(null);

    assertEquals(1, registry.getActiveSessionCount());
  }

  @Test
  void cleanupEndedSessions_removesInactiveSessions() {
    GameRegistry registry = new GameRegistry();
    PlayerSession p1 =
        registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));
    PlayerSession p2 =
        registry.registerPlayer("p2", "Alice", new PrintWriter(new StringWriter(), true));
    PlayerSession p3 =
        registry.registerPlayer("p3", "Bob", new PrintWriter(new StringWriter(), true));
    PlayerSession p4 =
        registry.registerPlayer("p4", "Eve", new PrintWriter(new StringWriter(), true));

    GameController controller = new DummyGameController();

    GameSession active = registry.createSession(List.of(p1, p2), controller);
    GameSession ended = registry.createSession(List.of(p3, p4), controller);
    ended.end(null);

    registry.cleanupEndedSessions();

    assertEquals(1, registry.getActiveSessionCount());
    assertTrue(registry.getActiveSessions().contains(active));
    assertFalse(registry.getActiveSessions().contains(ended));
  }

  @Test
  void removePlayer_endsAndRemovesActiveSessionContainingThatPlayer() {
    GameRegistry registry = new GameRegistry();
    PlayerSession p1 =
        registry.registerPlayer("p1", "Daniel", new PrintWriter(new StringWriter(), true));
    PlayerSession p2 =
        registry.registerPlayer("p2", "Alice", new PrintWriter(new StringWriter(), true));
    GameController controller = new DummyGameController();

    GameSession session = registry.createSession(List.of(p1, p2), controller);

    assertTrue(session.isActive());
    assertEquals(1, registry.getActiveSessionCount());

    registry.removePlayer("p1");

    assertNull(registry.getPlayer("p1"));
    assertEquals(1, registry.getPlayerCount());
    assertEquals(0, registry.getActiveSessionCount());
    assertNull(registry.getSessionForPlayer("p2"));
  }

  private static final class DummyGameController extends GameController {
    private DummyGameController() {
      super(new HeadlessView());
    }
  }
}