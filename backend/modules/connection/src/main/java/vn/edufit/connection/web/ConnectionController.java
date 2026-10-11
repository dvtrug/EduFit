package vn.edufit.connection.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;
import vn.edufit.connection.application.ConnectionService;
import vn.edufit.connection.application.dto.*;
import vn.edufit.connection.web.request.*;
import vn.edufit.connection.web.response.*;
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
    ParentStudentLinkResult link = service.respondInvitation(user(), body.invitationId(), body.accept());
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
    List<ConnectionRequestResult> result = service.requests(actor, studentId);
    if (actor.hasRole("TUTOR")) return ApiResponse.success(result.stream().map(r -> {
      ConnectionGoalDto goal = profile.findGoalForConnection(r.goalId())
          .orElseThrow(() -> EntityNotFoundException.of("LearningGoal", r.goalId()));
      String senderRole = iam.findUserSummaryById(r.sentByUserId())
          .map(u -> u.role() == AccountRole.PARENT ? "PARENT" : "STUDENT").orElse("STUDENT");
      return new TutorInboxResponse(r.requestId(), goal.goalType(), goal.subjectId(),
          goal.educationLevelId(), r.desiredSlots(), r.message(), senderRole,
          r.status(), r.expiresAt());
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
    ClassResult c = service.respondRequest(user(), id, body.accept(), body.reason());
    return ApiResponse.success(c == null ? null : ClassResponse.of(c));
  }
}
