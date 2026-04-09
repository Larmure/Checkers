package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.model.tools.Internationalization;
import org.junit.jupiter.api.Test;

class InvitationTest {

  @Test
  void newInvitationShouldBePending() {
    Invitation inv = new Invitation("INV-1", "alice", "bob");

    assertEquals("INV-1", inv.getInvitationId());
    assertEquals("alice", inv.getFromPlayerId());
    assertEquals("bob", inv.getToPlayerId());
    assertEquals(Invitation.InvitationStatus.PENDING, inv.getStatus());
    assertTrue(inv.isPending());
  }

  @Test
  void markAcceptedShouldChangeStatus() {
    Invitation inv = new Invitation("INV-1", "alice", "bob");

    inv.markAccepted();

    assertEquals(Invitation.InvitationStatus.ACCEPTED, inv.getStatus());
  }

  @Test
  void markDeclinedShouldChangeStatus() {
    Invitation inv = new Invitation("INV-1", "alice", "bob");

    inv.markDeclined();

    assertEquals(Invitation.InvitationStatus.DECLINED, inv.getStatus());
  }

  @Test
  void markCancelledShouldChangeStatus() {
    Invitation inv = new Invitation("INV-1", "alice", "bob");

    inv.markCancelled();

    assertEquals(Invitation.InvitationStatus.CANCELLED, inv.getStatus());
  }

  @Test
  void markExpiredShouldChangeStatus() {
    Invitation inv = new Invitation("INV-1", "alice", "bob");

    inv.markExpired();

    assertEquals(Invitation.InvitationStatus.EXPIRED, inv.getStatus());
  }

  @Test
  void remainingSecondsShouldNeverBeNegative() {
    Invitation inv = new Invitation("INV-1", "alice", "bob");

    assertTrue(inv.getRemainingSeconds() >= 0);
  }

  @Test
  void toString_containsLocalizedMainInformation() {
    Invitation inv = new Invitation("INV-1", "alice", "bob");

    String text = inv.toString();

    assertTrue(text.contains("INV-1"));
    assertTrue(text.contains("alice"));
    assertTrue(text.contains("bob"));
    assertTrue(text.contains(
        Internationalization.get("server.invitation.status.pending")));
  }
}