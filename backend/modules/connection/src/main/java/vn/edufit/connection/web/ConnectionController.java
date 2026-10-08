package vn.edufit.connection.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;
import vn.edufit.connection.application.ConnectionService;
import vn.edufit.connection.infra.persistence.*;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.api.IamFacade;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.ConnectionGoalDto;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ResourceConflictException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionController {
  private final ConnectionService service;
  private final ObjectProvider<CurrentUser> users;
  private final ProfileFacade profile;
  private final IamFacade iam;

  public ConnectionController(ConnectionService service, ObjectProvider<CurrentUser> users,
      ProfileFacade profile, IamFacade iam) {
    this.service = service;
    this.users = users;
    this.profile = profile;
    this.iam = iam;
  }

  private CurrentUser user() { return users.getObject(); }

  public record InviteBody(@NotBlank @Email String targetEmail) {}
  public record InvitationResponse(UUID invitationId, UUID inviterUserId, UUID invitedUserId,
      String status, Instant createdAt, Instant expiresAt) {
    static InvitationResponse of(LinkInvitation i) {
      return new InvitationResponse(i.getId(), i.getInviterUserId(), i.getInvitedUserId(),
          i.getStatus(), i.getCreatedAt(), i.getExpiresAt());
    }
  }
  public record RespondBody(@NotNull UUID invitationId, boolean accept) {}
  public record LinkResponse(UUID linkId, UUID parentUserId, UUID studentId, String status,
      Instant confirmedAt) {
    static LinkResponse of(ParentStudentLink l) {
      return new LinkResponse(l.getId(), l.getParentUserId(), l.getStudentId(),
          l.getStatus(), l.getConfirmedAt());
    }
  }
  public record RevokeBody(String reason) {}
  public record CreateRequestBody(@NotNull UUID goalId, @NotNull UUID tutorId,
      @NotNull List<String> desiredSlots, String message) {}
  public record RequestResponse(UUID requestId, UUID studentId, UUID tutorId, UUID goalId,
      String status, Instant createdAt, Instant expiresAt) {
    static RequestResponse of(ConnectionRequest r) {
      return new RequestResponse(r.getId(), r.getStudentId(), r.getTutorId(), r.getGoalId(),
          r.getStatus(), r.getCreatedAt(), r.getExpiresAt());
    }
  }
  public record TutorInboxResponse(UUID requestId, String goalType, Integer subjectId,
      Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
      String status, Instant expiresAt) {}
  public record RequestRespondBody(boolean accept, String reason) {}
  public record ClassResponse(UUID classId, UUID studentId, UUID tutorId, UUID goalId) {
    static ClassResponse of(TutoringClass c) {
      return new ClassResponse(c.getId(), c.getStudentId(), c.getTutorId(), c.getGoalId());
    }
  }

  @PostMapping("/parent-student/invite")
  public ApiResponse<Void> invite(@Valid @RequestBody InviteBody body) {
    if (!service.invite(user(), body.targetEmail()))
      throw new ResourceConflictException(ErrorCode.RATE_LIMIT_EXCEEDED,
          "Đã đạt giới hạn 10 lời mời trong một giờ");
    return ApiResponse.ok("Nếu tài khoản hợp lệ, lời mời sẽ được gửi");
  }

  @GetMapping("/parent-student/invitations")
  public ApiResponse<List<InvitationResponse>> invitations() {
    return ApiResponse.success(service.invitations(user()).stream().map(InvitationResponse::of).toList());
  }

  @PostMapping("/parent-student/respond")
  public ApiResponse<LinkResponse> respond(@Valid @RequestBody RespondBody body) {
    ParentStudentLink link = service.respondInvitation(user(), body.invitationId(), body.accept());
    return ApiResponse.success(link == null ? null : LinkResponse.of(link));
  }

  @PostMapping("/parent-student/invitations/{id}/withdraw")
  public ApiResponse<Void> withdraw(@PathVariable UUID id) {
    service.withdrawInvitation(user(), id);
    return ApiResponse.ok();
  }

  @GetMapping("/parent-student/links")
  public ApiResponse<List<LinkResponse>> links() {
    return ApiResponse.success(service.links(user()).stream().map(LinkResponse::of).toList());
  }

  @DeleteMapping("/parent-student/links/{id}")
  public ApiResponse<Void> revoke(@PathVariable UUID id, @RequestBody(required = false) RevokeBody body) {
    service.revokeLink(user(), id, body == null ? null : body.reason());
    return ApiResponse.ok();
  }

  @PostMapping("/requests")
  public ApiResponse<RequestResponse> request(@Valid @RequestBody CreateRequestBody body) {
    return ApiResponse.success(RequestResponse.of(service.request(user(), body.goalId(), body.tutorId(),
        body.desiredSlots(), body.message())));
  }

  @GetMapping("/requests")
  public ApiResponse<?> requests(@RequestParam(required = false) UUID studentId) {
    CurrentUser actor = user();
    List<ConnectionRequest> result = service.requests(actor, studentId);
    if (actor.hasRole("TUTOR")) return ApiResponse.success(result.stream().map(r -> {
      ConnectionGoalDto goal = profile.findGoalForConnection(r.getGoalId())
          .orElseThrow(() -> EntityNotFoundException.of("LearningGoal", r.getGoalId()));
      String senderRole = iam.findUserSummaryById(r.getSentByUserId())
          .map(u -> u.role() == AccountRole.PARENT ? "PARENT" : "STUDENT").orElse("STUDENT");
      return new TutorInboxResponse(r.getId(), goal.goalType(), goal.subjectId(),
          goal.educationLevelId(), r.getDesiredSlots(), r.getMessage(), senderRole,
          r.getStatus(), r.getExpiresAt());
    }).toList());
    return ApiResponse.success(result.stream().map(RequestResponse::of).toList());
  }

  @PostMapping("/requests/{id}/cancel")
  public ApiResponse<Void> cancel(@PathVariable UUID id) {
    service.cancelRequest(user(), id);
    return ApiResponse.ok();
  }

  @PostMapping("/requests/{id}/respond")
  public ApiResponse<ClassResponse> respondRequest(@PathVariable UUID id,
      @Valid @RequestBody RequestRespondBody body) {
    TutoringClass c = service.respondRequest(user(), id, body.accept(), body.reason());
    return ApiResponse.success(c == null ? null : ClassResponse.of(c));
  }
}
