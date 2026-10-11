package vn.edufit.connection.infra.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "link_invitation")
public class LinkInvitation {
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "invitation_id") private UUID id;
  @Column(name = "inviter_user_id", nullable = false) private UUID inviterUserId;
  @Column(name = "invited_user_id", nullable = false) private UUID invitedUserId;
  @Column(name = "status", nullable = false) private String status;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "expires_at", nullable = false) private Instant expiresAt;
  @Column(name = "responded_at") private Instant respondedAt;
  @Column(name = "reinvite_after") private Instant reinviteAfter;
  @Version @Column(name = "version", nullable = false) private Long version;

  protected LinkInvitation() {}

  public LinkInvitation(UUID inviter, UUID invited, Instant now) {
    inviterUserId = inviter;
    invitedUserId = invited;
    status = "PENDING";
    createdAt = now;
    expiresAt = now.plus(java.time.Duration.ofDays(14));
  }

  public void expire(Instant now) {
    if ("PENDING".equals(status) && !expiresAt.isAfter(now)) status = "EXPIRED";
  }

  public void accept(Instant now) { status = "ACCEPTED"; respondedAt = now; }
  public void decline(Instant now) {
    status = "DECLINED";
    respondedAt = now;
    reinviteAfter = now.plus(java.time.Duration.ofDays(7));
  }
  public void withdraw(Instant now) { status = "WITHDRAWN"; respondedAt = now; }

  public UUID getId() { return id; }
  public UUID getInviterUserId() { return inviterUserId; }
  public UUID getInvitedUserId() { return invitedUserId; }
  public String getStatus() { return status; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public Instant getReinviteAfter() { return reinviteAfter; }
  public Instant getRespondedAt() { return respondedAt; }
}
