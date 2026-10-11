package vn.edufit.connection.web.response;

import java.time.Instant;
import java.util.UUID;
import vn.edufit.connection.application.dto.InvitationResult;

public record InvitationResponse(UUID invitationId, UUID inviterUserId, UUID invitedUserId, String status, Instant createdAt, Instant expiresAt) {
  public static InvitationResponse of(InvitationResult r) {
    return new InvitationResponse(r.invitationId(), r.inviterUserId(), r.invitedUserId(), r.status(), r.createdAt(), r.expiresAt());
  }
}
