package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvitationManagerTest {

  private GameRegistry registry;
  private InvitationManager manager;
  private PlayerSession alice;
  private PlayerSession bob;

  @BeforeEach
  void setUp() {
    registry = new GameRegistry();
    manager = new InvitationManager();

    alice = registry.registerPlayer("alice", "Alice", new PrintWriter(new StringWriter(), true));
    bob = registry.registerPlayer("bob", "Bob", new PrintWriter(new StringWriter(), true));
  }

  @Test
  void createInvitationShouldSucceedWhenBothPlayersAreIdle() {
    InvitationManager.CreateResult result = manager.createInvitation(alice, "bob", registry);

    assertTrue(result.isSuccess());
    assertNotNull(result.invitation);
    assertEquals("alice", result.invitation.getFromPlayerId());
    assertEquals("bob", result.invitation.getToPlayerId());
    assertEquals(PlayerSession.Status.WAITGAME, bob.getStatus());
  }

  @Test
  void createInvitationShouldFailWhenTargetDoesNotExist() {
    InvitationManager.CreateResult result = manager.createInvitation(alice, "charlie", registry);

    assertFalse(result.isSuccess());
    assertNull(result.invitation);
    assertTrue(result.error.contains("not found"));
  }

  @Test
  void createInvitationShouldFailWhenTargetIsAway() {
    bob.setStatus(PlayerSession.Status.AWAY);

    InvitationManager.CreateResult result = manager.createInvitation(alice, "bob", registry);

    assertFalse(result.isSuccess());
    assertTrue(result.error.contains("away"));
  }

  @Test
  void createInvitationShouldFailWhenInvitingSelf() {
    InvitationManager.CreateResult result = manager.createInvitation(alice, "alice", registry);

    assertFalse(result.isSuccess());
    assertTrue(result.error.contains("cannot invite yourself"));
  }

  @Test
  void createInvitationShouldFailWhenTargetIsNoLongerIdleAfterFirstInvitation() {
    InvitationManager.CreateResult first = manager.createInvitation(alice, "bob", registry);
    InvitationManager.CreateResult second = manager.createInvitation(alice, "bob", registry);

    assertTrue(first.isSuccess());
    assertFalse(second.isSuccess());
    assertTrue(second.error.contains("already in a game"));
  }

  @Test
  void acceptShouldSucceedWhenInvitationExists() {
    manager.createInvitation(alice, "bob", registry);

    InvitationManager.AcceptResult result = manager.accept("bob");

    assertTrue(result.isSuccess());
    assertNotNull(result.invitation);
    assertEquals(Invitation.InvitationStatus.ACCEPTED, result.invitation.getStatus());
  }

  @Test
  void acceptShouldFailWhenNoInvitationExists() {
    InvitationManager.AcceptResult result = manager.accept("bob");

    assertFalse(result.isSuccess());
    assertNull(result.invitation);
    assertTrue(result.error.contains("No pending invitation"));
  }

  @Test
  void declineShouldSucceedWhenInvitationExists() {
    manager.createInvitation(alice, "bob", registry);

    InvitationManager.DeclineResult result = manager.decline("bob");

    assertTrue(result.isSuccess());
    assertEquals(Invitation.InvitationStatus.DECLINED, result.invitation.getStatus());
  }

  @Test
  void declineShouldFailWhenNoInvitationExists() {
    InvitationManager.DeclineResult result = manager.decline("bob");

    assertFalse(result.isSuccess());
    assertNull(result.invitation);
  }

  @Test
  void cancelShouldSucceedWhenOutgoingInvitationExists() {
    manager.createInvitation(alice, "bob", registry);

    InvitationManager.CancelResult result = manager.cancel("alice");

    assertTrue(result.isSuccess());
    assertEquals(Invitation.InvitationStatus.CANCELLED, result.invitation.getStatus());
  }

  @Test
  void cancelShouldFailWhenNoOutgoingInvitationExists() {
    InvitationManager.CancelResult result = manager.cancel("alice");

    assertFalse(result.isSuccess());
    assertNull(result.invitation);
  }

  @Test
  void findPendingToShouldReturnInvitationForInvitee() {
    manager.createInvitation(alice, "bob", registry);

    Invitation invitation = manager.findPendingTo("bob");

    assertNotNull(invitation);
    assertEquals("alice", invitation.getFromPlayerId());
  }

  @Test
  void findPendingFromShouldReturnInvitationForInviter() {
    manager.createInvitation(alice, "bob", registry);

    Invitation invitation = manager.findPendingFrom("alice");

    assertNotNull(invitation);
    assertEquals("bob", invitation.getToPlayerId());
  }
}