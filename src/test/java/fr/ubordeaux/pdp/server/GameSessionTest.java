package fr.ubordeaux.pdp.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.view.HeadlessView;

class GameSessionTest {

  private PlayerSession alice;
  private PlayerSession bob;
  private FakeGameController controller;
  private GameSession session;

  @BeforeEach
  void setUp() {
    alice = new PlayerSession(
        "alice", "Alice", new PrintWriter(new StringWriter(), true), "GUI");
    bob = new PlayerSession(
        "bob", "Bob", new PrintWriter(new StringWriter(), true), "GUI");
    controller = new FakeGameController();
    session = new GameSession(List.of(alice, bob), controller);
  }

  @Test
  void constructorShouldCreateActiveSessionAndSetPlayersInGame() {
    assertTrue(session.isActive());
    assertNotNull(session.getSessionId());
    assertEquals(PlayerSession.Status.INGAME, alice.getStatus());
    assertEquals(PlayerSession.Status.INGAME, bob.getStatus());
    assertEquals(alice, session.getCurrentPlayer());
    assertEquals(2, session.getPlayers().size());
  }

  @Test
  void hasPlayerShouldReturnTrueForRegisteredPlayers() {
    assertTrue(session.hasPlayer("alice"));
    assertTrue(session.hasPlayer("bob"));
    assertFalse(session.hasPlayer("charlie"));
  }

  @Test
  void handleMoveShouldRejectMoveWhenSessionIsInactive() {
    session.end("alice");

    String error = session.handleMove(alice, "b6-a5");

    assertNotNull(error);
    assertTrue(error.contains("no longer active"));
  }

  @Test
  void handleMoveShouldRejectMoveWhenItIsNotPlayersTurn() {
    String error = session.handleMove(bob, "b6-a5");

    assertNotNull(error);
    assertTrue(error.contains("Not your turn"));
    assertEquals(alice, session.getCurrentPlayer());
  }

  @Test
  void handleMoveShouldRejectInvalidFormat() {
    String error = session.handleMove(alice, "b6a5");

    assertNotNull(error);
    assertTrue(error.contains("Invalid move format"));
    assertEquals(alice, session.getCurrentPlayer());
  }

  @Test
  void handleMoveShouldRejectIllegalMoveWhenControllerThrows() {
    controller.setException(new IllegalArgumentException("forbidden move"));

    String error = session.handleMove(alice, "b6-a5");

    assertNotNull(error);
    assertTrue(error.contains("Illegal move"));
    assertTrue(error.contains("forbidden move"));
    assertEquals(alice, session.getCurrentPlayer());
  }

  @Test
  void handleMoveShouldAcceptLegalMoveAndAdvanceTurn() {
    String error = session.handleMove(alice, "b6-a5");

    assertNull(error);
    assertEquals("b6", controller.lastFrom);
    assertEquals("a5", controller.lastTo);
    assertFalse(controller.lastIsManoury);
    assertEquals(bob, session.getCurrentPlayer());
  }

  @Test
  void handleMoveShouldAlternateTurnsAfterTwoValidMoves() {
    assertNull(session.handleMove(alice, "b6-a5"));
    assertEquals(bob, session.getCurrentPlayer());

    assertNull(session.handleMove(bob, "c3-d4"));
    assertEquals(alice, session.getCurrentPlayer());
  }

  @Test
  void endShouldRecordWinnerAndLoserAndSetPlayersIdle() {
    session.end("alice");

    assertFalse(session.isActive());

    assertEquals(1, alice.getWins());
    assertEquals(0, alice.getLosses());
    assertEquals(0, alice.getDraws());

    assertEquals(0, bob.getWins());
    assertEquals(1, bob.getLosses());
    assertEquals(0, bob.getDraws());

    assertEquals(PlayerSession.Status.IDLE, alice.getStatus());
    assertEquals(PlayerSession.Status.IDLE, bob.getStatus());
  }

  @Test
  void endShouldRecordDrawForAllPlayersWhenWinnerIsNull() {
    session.end(null);

    assertFalse(session.isActive());

    assertEquals(0, alice.getWins());
    assertEquals(0, alice.getLosses());
    assertEquals(1, alice.getDraws());

    assertEquals(0, bob.getWins());
    assertEquals(0, bob.getLosses());
    assertEquals(1, bob.getDraws());

    assertEquals(PlayerSession.Status.IDLE, alice.getStatus());
    assertEquals(PlayerSession.Status.IDLE, bob.getStatus());
  }

  @Test
  void endShouldDoNothingWhenCalledTwice() {
    session.end("alice");
    session.end("bob");

    assertEquals(1, alice.getWins());
    assertEquals(0, alice.getLosses());

    assertEquals(0, bob.getWins());
    assertEquals(1, bob.getLosses());
  }

  @Test
  void toStringShouldContainSessionIdAndPlayersAndCurrentTurn() {
    String text = session.toString();

    assertTrue(text.contains("GAME-"));
    assertTrue(text.contains("alice"));
    assertTrue(text.contains("bob"));
    assertTrue(text.contains("turn=alice"));
  }

  /**
   * Faux contrôleur minimal pour tester GameSession sans dépendre
   * de la vraie logique du jeu.
   */
  private static class FakeGameController extends GameController {

    private String lastFrom;
    private String lastTo;
    private boolean lastIsManoury;
    private RuntimeException exceptionToThrow;

    FakeGameController() {
      super(new HeadlessView());
    }

    void setException(RuntimeException exception) {
      this.exceptionToThrow = exception;
    }

    @Override
    public void executeMove(String from, String to, boolean isManoury) {
      if (exceptionToThrow != null) {
        throw exceptionToThrow;
      }
      this.lastFrom = from;
      this.lastTo = to;
      this.lastIsManoury = isManoury;
    }
  }
}