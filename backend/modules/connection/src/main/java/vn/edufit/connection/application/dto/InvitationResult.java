package vn.edufit.connection.application.dto;

import java.time.Instant;
import java.util.UUID;
import vn.edufit.connection.infra.persistence.entity.LinkInvitation;

public record InvitationResult(UUID invitationId, UUID inviterUserId, UUID invitedUserId, String status, Instant createdAt, Instant expiresAt) {
  public static InvitationResult from(LinkInvitation r) {
    return new InvitationResult(r.getId(), r.getInviterUserId(), r.getInvitedUserId(), r.getStatus(), r.getCreatedAt(), r.getExpiresAt());
  }
}
