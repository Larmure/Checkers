package fr.ubordeaux.pdp.server;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Thread-safe manager for all pending game invitations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Create and store {@link Invitation} objects.
 *   <li>Enforce business rules (can't invite an {@code away} player, can't invite
 *       someone already in a game, only one pending invitation per pair, etc.).
 *   <li>Run a background sweep every 30 s that marks expired invitations and calls the
 *       provided {@code expiryCallback} so the server can notify the affected players.
 * </ul>
 *
 * <p>The expiry callback receives {@code (invitation, registry)} so it can look up the
 * live {@link PlayerSession} objects and push {@code INVITATION_EXPIRED} messages.
 */
public class InvitationManager {

  private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);
  private static final int SWEEP_PERIOD_SECONDS = 30;

  /** All known invitations, keyed by their unique ID. */
  private final Map<String, Invitation> invitations = new ConcurrentHashMap<>();

  /** Scheduled sweep task for expiry checks. */
  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "invitation-sweeper");
            t.setDaemon(true);
            return t;
          });

  /**
   * Starts the background expiry sweeper.
   *
   * @param registry       the player registry used to send expiry notifications.
   * @param expiryCallback called for each invitation that has just been marked expired.
   *                       Receives the expired invitation and the registry.
   */
  public void startSweeper(GameRegistry registry,
      BiConsumer<Invitation, GameRegistry> expiryCallback) {
    scheduler.scheduleAtFixedRate(
        () -> sweepExpired(registry, expiryCallback),
        SWEEP_PERIOD_SECONDS, SWEEP_PERIOD_SECONDS, TimeUnit.SECONDS);
  }

  /** Stops the background sweeper (called on server shutdown). */
  public void stopSweeper() {
    scheduler.shutdownNow();
  }

  /**
   * Creates and stores a new invitation if all pre-conditions are satisfied.
   *
   * <p>Pre-conditions checked (in order):
   * <ol>
   *   <li>Invitee exists in the registry.
   *   <li>Invitee is not {@code away}.
   *   <li>Invitee is {@code idle} (not in a game).
   *   <li>No pending invitation already exists between the same pair.
   * </ol>
   *
   * @param fromPlayer  the player sending the invitation.
   * @param toPlayerId  the target player's ID.
   * @param registry    the live player registry.
   * @return the newly created {@link Invitation}, or {@code null} if a pre-condition failed.
   *         Callers must check the returned value and the {@code errorOut} consumer.
   */
  public synchronized CreateResult createInvitation(
      PlayerSession fromPlayer, String toPlayerId, GameRegistry registry) {

    PlayerSession toPlayer = registry.getPlayer(toPlayerId);

    if (toPlayer == null) {
      return CreateResult.error("Player '" + toPlayerId + "' not found.");
    }
    if (toPlayer.getStatus() == PlayerSession.Status.AWAY) {
      return CreateResult.error(
          "Player '" + toPlayerId + "' is away and cannot receive invitations.");
    }
    if (!toPlayer.isIdle()) {
      return CreateResult.error("Player '" + toPlayerId + "' is already in a game.");
    }
    if (fromPlayer.getId().equals(toPlayerId)) {
      return CreateResult.error("You cannot invite yourself.");
    }

    // Check for an existing pending invitation between this pair (either direction)
    boolean duplicate = invitations.values().stream()
        .anyMatch(inv -> inv.isPending()
            && inv.getFromPlayerId().equals(fromPlayer.getId())
            && inv.getToPlayerId().equals(toPlayerId));
    if (duplicate) {
      return CreateResult.error(
          "A pending invitation to '" + toPlayerId + "' already exists. "
              + "Use 'cancel' to withdraw it first.");
    }

    String id = "INV-" + ID_COUNTER.getAndIncrement();
    Invitation invitation = new Invitation(id, fromPlayer.getId(), toPlayerId);
    invitations.put(id, invitation);

    // Transition the invitee to WAITGAME status
    toPlayer.setStatus(PlayerSession.Status.WAITGAME);

    return CreateResult.success(invitation);
  }

  /**
   * Accepts the pending invitation addressed to {@code acceptingPlayerId}.
   *
   * @param acceptingPlayerId the player calling {@code ACCEPT}.
   * @return the accepted invitation, or {@code null} with error text if not found / invalid.
   */
  public synchronized AcceptResult accept(String acceptingPlayerId) {
    Invitation inv = findPendingTo(acceptingPlayerId);

    if (inv == null) {
      return AcceptResult.error("No pending invitation found for you. "
          + "Wait for another player to invite you with 'new <your_id>'.");
    }
    if (inv.isExpired()) {
      inv.markExpired();
      return AcceptResult.error("That invitation has already expired.");
    }

    inv.markAccepted();
    return AcceptResult.success(inv);
  }

  /**
   * Declines the pending invitation addressed to {@code decliningPlayerId}.
   *
   * @param decliningPlayerId the player calling {@code DECLINE}.
   * @return the declined invitation, or an error result.
   */
  public synchronized DeclineResult decline(String decliningPlayerId) {
    Invitation inv = findPendingTo(decliningPlayerId);

    if (inv == null) {
      return DeclineResult.error("No pending invitation found for you.");
    }

    inv.markDeclined();
    return DeclineResult.success(inv);
  }

  /**
   * Cancels any pending invitation sent <em>by</em> {@code cancellingPlayerId}.
   *
   * @param cancellingPlayerId the player calling {@code CANCEL}.
   * @return the cancelled invitation, or an error result.
   */
  public synchronized CancelResult cancel(String cancellingPlayerId) {
    Invitation inv = findPendingFrom(cancellingPlayerId);

    if (inv == null) {
      return CancelResult.error("You have no pending outgoing invitation to cancel.");
    }

    inv.markCancelled();
    return CancelResult.success(inv);
  }

  /**
   * Returns the first pending invitation <em>sent to</em> the given player.
   *
   * @param toPlayerId target player ID.
   * @return the invitation, or {@code null}.
   */
  public Invitation findPendingTo(String toPlayerId) {
    return invitations.values().stream()
        .filter(inv -> inv.isPending() && inv.getToPlayerId().equals(toPlayerId))
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns the first pending invitation <em>sent by</em> the given player.
   *
   * @param fromPlayerId sender player ID.
   * @return the invitation, or {@code null}.
   */
  public Invitation findPendingFrom(String fromPlayerId) {
    return invitations.values().stream()
        .filter(inv -> inv.isPending() && inv.getFromPlayerId().equals(fromPlayerId))
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns all invitations (including historical ones) — useful for debugging.
   *
   * @return unmodifiable view of all invitations.
   */
  public Collection<Invitation> getAllInvitations() {
    return List.copyOf(invitations.values());
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  /**
   * Marks all pending-but-expired invitations as expired and fires the callback.
   */
  private void sweepExpired(GameRegistry registry,
      BiConsumer<Invitation, GameRegistry> expiryCallback) {
    invitations.values().forEach(inv -> {
      if (inv.getStatus() == Invitation.InvitationStatus.PENDING && inv.isExpired()) {
        inv.markExpired();
        // Restore invitee to IDLE
        PlayerSession invitee = registry.getPlayer(inv.getToPlayerId());
        if (invitee != null && invitee.getStatus() == PlayerSession.Status.WAITGAME) {
          invitee.setStatus(PlayerSession.Status.IDLE);
        }
        expiryCallback.accept(inv, registry);
      }
    });
  }

  // -------------------------------------------------------------------------
  // Result value objects (simple sealed alternatives to exceptions)
  // -------------------------------------------------------------------------

  /** Result of {@link #createInvitation}. */
  public static final class CreateResult {
    public final Invitation invitation;
    public final String error;

    private CreateResult(Invitation invitation, String error) {
      this.invitation = invitation;
      this.error = error;
    }

    public static CreateResult success(Invitation inv) {
      return new CreateResult(inv, null);
    }

    public static CreateResult error(String msg) {
      return new CreateResult(null, msg);
    }

    public boolean isSuccess() {
      return invitation != null;
    }
  }

  /** Result of {@link #accept}. */
  public static final class AcceptResult {
    public final Invitation invitation;
    public final String error;

    private AcceptResult(Invitation invitation, String error) {
      this.invitation = invitation;
      this.error = error;
    }

    public static AcceptResult success(Invitation inv) {
      return new AcceptResult(inv, null);
    }

    public static AcceptResult error(String msg) {
      return new AcceptResult(null, msg);
    }

    public boolean isSuccess() {
      return invitation != null;
    }
  }

  /** Result of {@link #decline}. */
  public static final class DeclineResult {
    public final Invitation invitation;
    public final String error;

    private DeclineResult(Invitation invitation, String error) {
      this.invitation = invitation;
      this.error = error;
    }

    public static DeclineResult success(Invitation inv) {
      return new DeclineResult(inv, null);
    }

    public static DeclineResult error(String msg) {
      return new DeclineResult(null, msg);
    }

    public boolean isSuccess() {
      return invitation != null;
    }
  }

  /** Result of {@link #cancel}. */
  public static final class CancelResult {
    public final Invitation invitation;
    public final String error;

    private CancelResult(Invitation invitation, String error) {
      this.invitation = invitation;
      this.error = error;
    }

    public static CancelResult success(Invitation inv) {
      return new CancelResult(inv, null);
    }

    public static CancelResult error(String msg) {
      return new CancelResult(null, msg);
    }

    public boolean isSuccess() {
      return invitation != null;
    }
  }
}