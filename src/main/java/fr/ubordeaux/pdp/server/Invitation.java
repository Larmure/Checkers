package fr.ubordeaux.pdp.server;

import java.time.Instant;

/**
 * Represents a pending game invitation from one player to another.
 *
 * <p>An invitation expires automatically after {@value #TIMEOUT_SECONDS} seconds
 * if neither {@code accept} nor {@code decline} is called.
 *
 * <p>Lifecycle:
 * <pre>
 *   PENDING  →  ACCEPTED  (invitee calls accept)
 *   PENDING  →  DECLINED  (invitee calls decline)
 *   PENDING  →  CANCELLED (inviter calls cancel)
 *   PENDING  →  EXPIRED   (timeout reached with no response)
 * </pre>
 */
public class Invitation {

  /** Duration before an unanswered invitation expires (seconds). */
  public static final int TIMEOUT_SECONDS = 300; // 5 minutes

  /** Possible states of an invitation. */
  public enum InvitationStatus {
    /** Invitation has been created and is awaiting a response. */
    PENDING,
    /** Invitee accepted the invitation. */
    ACCEPTED,
    /** Invitee explicitly declined the invitation. */
    DECLINED,
    /** Inviter cancelled the invitation before it was answered. */
    CANCELLED,
    /** Invitation timed out before any response was received. */
    EXPIRED
  }

  private final String invitationId;
  private final String fromPlayerId;
  private final String toPlayerId;
  private final Instant createdAt;

  private volatile InvitationStatus status = InvitationStatus.PENDING;

  /**
   * Creates a new invitation.
   *
   * @param invitationId a unique identifier for this invitation.
   * @param fromPlayerId the player who sent the invitation.
   * @param toPlayerId   the player who received the invitation.
   */
  public Invitation(String invitationId, String fromPlayerId, String toPlayerId) {
    this.invitationId = invitationId;
    this.fromPlayerId = fromPlayerId;
    this.toPlayerId = toPlayerId;
    this.createdAt = Instant.now();
  }

  /**
   * Returns the unique invitation identifier.
   *
   * @return the unique invitation identifier.
   */
  public String getInvitationId() {
    return invitationId;
  }

  /**
   * Returns the ID of the player who sent the invitation.
   *
   * @return the inviter's player ID.
   */
  public String getFromPlayerId() {
    return fromPlayerId;
  }

  /**
   * Returns the ID of the player who received the invitation.
   *
   * @return the invitee's player ID.
   */
  public String getToPlayerId() {
    return toPlayerId;
  }

  /**
   * Returns the current status of the invitation.
   *
   * @return the current {@link InvitationStatus}.
   */
  public InvitationStatus getStatus() {
    return status;
  }

  /**
   * Returns whether the invitation is still pending (awaiting a response and not expired).
   *
   * @return {@code true} if the invitation is pending.
   */
  public boolean isPending() {
    return status == InvitationStatus.PENDING && !isExpired();
  }

  /**
   * Returns whether the invitation has passed its timeout window.
   *
   * <p>This check is purely time-based; the status field is not updated automatically.
   * Call {@link #markExpired()} to persist the expiry state.
   *
   * @return {@code true} if more than {@value #TIMEOUT_SECONDS} seconds have elapsed.
   */
  public boolean isExpired() {
    return Instant.now().isAfter(createdAt.plusSeconds(TIMEOUT_SECONDS));
  }

  /**
   * Returns the number of seconds remaining before this invitation expires.
   * Returns 0 if already expired.
   *
   * @return remaining seconds, or 0 if expired.
   */
  public long getRemainingSeconds() {
    long remaining = TIMEOUT_SECONDS
        - java.time.Duration.between(createdAt, Instant.now()).getSeconds();
    return Math.max(0, remaining);
  }

  /** Transitions the invitation to {@link InvitationStatus#ACCEPTED}. */
  public synchronized void markAccepted() {
    if (status == InvitationStatus.PENDING) {
      status = InvitationStatus.ACCEPTED;
    }
  }

  /** Transitions the invitation to {@link InvitationStatus#DECLINED}. */
  public synchronized void markDeclined() {
    if (status == InvitationStatus.PENDING) {
      status = InvitationStatus.DECLINED;
    }
  }

  /** Transitions the invitation to {@link InvitationStatus#CANCELLED}. */
  public synchronized void markCancelled() {
    if (status == InvitationStatus.PENDING) {
      status = InvitationStatus.CANCELLED;
    }
  }

  /** Transitions the invitation to {@link InvitationStatus#EXPIRED}. */
  public synchronized void markExpired() {
    if (status == InvitationStatus.PENDING) {
      status = InvitationStatus.EXPIRED;
    }
  }

  @Override
  public String toString() {
    return "Invitation["
        + invitationId
        + " from=" + fromPlayerId
        + " to=" + toPlayerId
        + " status=" + status
        + " remaining=" + getRemainingSeconds() + "s]";
  }
}