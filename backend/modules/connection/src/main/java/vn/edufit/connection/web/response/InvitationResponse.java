package vn.edufit.connection.web.response;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import vn.edufit.connection.application.dto.InvitationResult;

public record InvitationResponse(UUID invitationId, UUID inviterUserId, UUID invitedUserId, String status, Instant createdAt, Instant expiresAt, @JsonInclude(JsonInclude.Include.NON_NULL) Instant reinviteAfter) {
  public static InvitationResponse of(InvitationResult r) {
    return new InvitationResponse(r.invitationId(), r.inviterUserId(), r.invitedUserId(), r.status(), r.createdAt(), r.expiresAt(), r.reinviteAfter());
  }
}
