package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.model.tools.Internationalization;
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
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
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
      return CreateResult.error(
          Internationalization.get("server.invitation.player_not_found", toPlayerId));
    }
    if (toPlayer.getStatus() == PlayerSession.Status.AWAY) {
      return CreateResult.error(
          Internationalization.get("server.invitation.player_away", toPlayerId));
    }
    if (!toPlayer.isIdle()) {
      return CreateResult.error(
          Internationalization.get("server.invitation.player_already_ingame", toPlayerId));
    }
    if (fromPlayer.getId().equals(toPlayerId)) {
      return CreateResult.error(
          Internationalization.get("server.invitation.cannot_invite_self"));
    }

    boolean duplicate = invitations.values().stream()
        .anyMatch(inv -> inv.isPending()
            && inv.getFromPlayerId().equals(fromPlayer.getId())
            && inv.getToPlayerId().equals(toPlayerId));
    if (duplicate) {
      return CreateResult.error(
          Internationalization.get("server.invitation.duplicate_pending", toPlayerId));
    }

    String id = "INV-" + ID_COUNTER.getAndIncrement();
    Invitation invitation = new Invitation(id, fromPlayer.getId(), toPlayerId);
    invitations.put(id, invitation);

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
      return AcceptResult.error(
          Internationalization.get("server.invitation.accept.none_found"));
    }
    if (inv.isExpired()) {
      inv.markExpired();
      return AcceptResult.error(
          Internationalization.get("server.invitation.already_expired"));
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
      return DeclineResult.error(
          Internationalization.get("server.invitation.decline.none_found"));
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
      return CancelResult.error(
          Internationalization.get("server.invitation.cancel.none_found"));
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
        PlayerSession invitee = registry.getPlayer(inv.getToPlayerId());
        if (invitee != null && invitee.getStatus() == PlayerSession.Status.WAITGAME) {
          invitee.setStatus(PlayerSession.Status.IDLE);
        }
        expiryCallback.accept(inv, registry);
      }
    });
  }

  /** Result of {@link #createInvitation}. */
  public static final class CreateResult {
    /** Created invitation when the operation succeeds; {@code null} otherwise. */
    public final Invitation invitation;
    /** Error message when the operation fails; {@code null} on success. */
    public final String error;

    private CreateResult(Invitation invitation, String error) {
      this.invitation = invitation;
      this.error = error;
    }

    /**
     * Creates a successful result.
     *
     * @param inv created invitation
     * @return a successful create result
     */
    public static CreateResult success(Invitation inv) {
      return new CreateResult(inv, null);
    }

    /**
     * Creates a failed result.
     *
     * @param msg error message
     * @return a failed create result
     */
    public static CreateResult error(String msg) {
      return new CreateResult(null, msg);
    }

    /**
     * Indicates whether invitation creation succeeded.
     *
     * @return {@code true} when {@link #invitation} is non-null
     */
    public boolean isSuccess() {
      return invitation != null;
    }
  }

  /** Result of {@link #accept}. */
  public static final class AcceptResult {
    /** Accepted invitation when the operation succeeds; {@code null} otherwise. */
    public final Invitation invitation;
    /** Error message when the operation fails; {@code null} on success. */
    public final String error;

    private AcceptResult(Invitation invitation, String error) {
      this.invitation = invitation;
      this.error = error;
    }

    /**
     * Creates a successful result.
     *
     * @param inv accepted invitation
     * @return a successful accept result
     */
    public static AcceptResult success(Invitation inv) {
      return new AcceptResult(inv, null);
    }

    /**
     * Creates a failed result.
     *
     * @param msg error message
     * @return a failed accept result
     */
    public static AcceptResult error(String msg) {
      return new AcceptResult(null, msg);
    }

    /**
     * Indicates whether invitation acceptance succeeded.
     *
     * @return {@code true} when {@link #invitation} is non-null
     */
    public boolean isSuccess() {
      return invitation != null;
    }
  }

  /** Result of {@link #decline}. */
  public static final class DeclineResult {
    /** Declined invitation when the operation succeeds; {@code null} otherwise. */
    public final Invitation invitation;
    /** Error message when the operation fails; {@code null} on success. */
    public final String error;

    private DeclineResult(Invitation invitation, String error) {
      this.invitation = invitation;
      this.error = error;
    }

    /**
     * Creates a successful result.
     *
     * @param inv declined invitation
     * @return a successful decline result
     */
    public static DeclineResult success(Invitation inv) {
      return new DeclineResult(inv, null);
    }

    /**
     * Creates a failed result.
     *
     * @param msg error message
     * @return a failed decline result
     */
    public static DeclineResult error(String msg) {
      return new DeclineResult(null, msg);
    }

    /**
     * Indicates whether invitation decline succeeded.
     *
     * @return {@code true} when {@link #invitation} is non-null
     */
    public boolean isSuccess() {
      return invitation != null;
    }
  }

  /** Result of {@link #cancel}. */
  public static final class CancelResult {
    /** Cancelled invitation when the operation succeeds; {@code null} otherwise. */
    public final Invitation invitation;
    /** Error message when the operation fails; {@code null} on success. */
    public final String error;

    private CancelResult(Invitation invitation, String error) {
      this.invitation = invitation;
      this.error = error;
    }

    /**
     * Creates a successful result.
     *
     * @param inv cancelled invitation
     * @return a successful cancel result
     */
    public static CancelResult success(Invitation inv) {
      return new CancelResult(inv, null);
    }

    /**
     * Creates a failed result.
     *
     * @param msg error message
     * @return a failed cancel result
     */
    public static CancelResult error(String msg) {
      return new CancelResult(null, msg);
    }

    /**
     * Indicates whether invitation cancellation succeeded.
     *
     * @return {@code true} when {@link #invitation} is non-null
     */
    public boolean isSuccess() {
      return invitation != null;
    }
  }
}