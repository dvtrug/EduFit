package vn.edufit.connection.application.service;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.connection.application.dto.InvitationResult;
import vn.edufit.connection.application.dto.ParentStudentLinkResult;
import vn.edufit.connection.application.dto.ConnectionRequestResult;
import vn.edufit.connection.application.dto.TutorInboxResult;
import vn.edufit.connection.application.dto.PageResponse;
import vn.edufit.connection.infra.persistence.entity.LinkInvitation;
import vn.edufit.connection.infra.persistence.entity.ConnectionRequest;
import vn.edufit.connection.infra.persistence.entity.ParentStudentLink;
import vn.edufit.connection.infra.persistence.repository.LinkInvitationRepository;
import vn.edufit.connection.infra.persistence.repository.ParentStudentLinkRepository;
import vn.edufit.connection.infra.persistence.repository.ConnectionRequestRepository;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.api.IamFacade;
import vn.edufit.iam.api.dto.UserSummaryView;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.ConnectionGoalDto;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ResourceConflictException;
import vn.edufit.shared.exception.ErrorCode;

@Service
public class ConnectionQueryService {
  private final Clock clock;
  private final LinkInvitationRepository invitations;
  private final ParentStudentLinkRepository links;
  private final ConnectionRequestRepository requests;
  private final ProfileFacade profile;
  private final IamFacade iam;
  private final ConnectionFacade facade;
  private final ConnectionExpiryService expiryService;

  @org.springframework.beans.factory.annotation.Autowired
  public ConnectionQueryService(LinkInvitationRepository invitations, ParentStudentLinkRepository links,
      ConnectionRequestRepository requests, ProfileFacade profile, IamFacade iam, ConnectionFacade facade,
      ConnectionExpiryService expiryService, Clock clock) {
    this.clock = clock;
    this.invitations = invitations;
    this.links = links;
    this.requests = requests;
    this.profile = profile;
    this.iam = iam;
    this.facade = facade;
    this.expiryService = expiryService;
  }

  public ConnectionQueryService(LinkInvitationRepository invitations, ParentStudentLinkRepository links,
      ConnectionRequestRepository requests, ProfileFacade profile, IamFacade iam, ConnectionFacade facade, Clock clock) {
    this(invitations, links, requests, profile, iam, facade, null, clock);
  }

  private static void requireRole(CurrentUser actor, String... roles) {
    for (String role : roles) if (actor.hasRole(role)) return;
    throw new ForbiddenOperationException("Không có quyền thực hiện thao tác này");
  }

  private void requireStudentAccess(CurrentUser actor, UUID studentId) {
    if (actor.hasRole("STUDENT") && profile.findStudentById(studentId)
        .map(s -> s.userId().equals(actor.getUserId())).orElse(false)) return;
    if (actor.hasRole("PARENT") && facade.hasConfirmedParentLink(actor.getUserId(), studentId)) return;
    throw new ForbiddenOperationException("Không có quyền với học sinh này");
  }

  @Transactional
  public List<InvitationResult> invitations(CurrentUser actor) {
    requireRole(actor, "PARENT", "STUDENT");
    List<LinkInvitation> result = invitations.findByInviterUserIdOrInvitedUserIdOrderByCreatedAtDesc(
        actor.getUserId(), actor.getUserId());
    Instant now = clock.instant();
    for (LinkInvitation i : result) {
      if ("PENDING".equals(i.getStatus()) && vn.edufit.connection.application.policy.ConnectionTransitions.isDue(i.getExpiresAt(), now)) {
        if (expiryService != null) expiryService.expireInvitation(i.getId(), now);
        i.expire(now);
      }
    }
    if (result.isEmpty()) {
      return List.of();
    }
    List<UUID> inviterUserIds = result.stream().map(LinkInvitation::getInviterUserId).distinct().toList();
    Map<UUID, UserSummaryView> inviterSummaries = iam.findUserSummariesByIds(inviterUserIds);
    return result.stream().map(i -> {
      UserSummaryView inviter = inviterSummaries.get(i.getInviterUserId());
      String inviterName = inviter != null ? inviter.fullName() : null;
      String inviterRole = (inviter != null && inviter.role() != null) ? inviter.role().name() : null;
      return InvitationResult.from(i, inviterName, inviterRole);
    }).toList();
  }

  @Transactional(readOnly = true)
  public List<ParentStudentLinkResult> links(CurrentUser actor) {
    requireRole(actor, "PARENT", "STUDENT");
    if (actor.hasRole("PARENT")) return links.findByParentUserIdAndStatus(actor.getUserId(), "CONFIRMED").stream().map(ParentStudentLinkResult::from).toList();
    UUID studentId = profile.getStudentIdByUserId(actor.getUserId());
    return links.findByStudentIdAndStatus(studentId, "CONFIRMED").stream().map(ParentStudentLinkResult::from).toList();
  }

  @Transactional
  public List<ConnectionRequestResult> requests(CurrentUser actor, UUID studentId) {
    requireRole(actor, "STUDENT", "PARENT");
    UUID scope = actor.hasRole("STUDENT") ? profile.getStudentIdByUserId(actor.getUserId()) : studentId;
    if (scope == null) throw new InvalidOperationException("Cần chỉ định học sinh");
    requireStudentAccess(actor, scope);
    List<ConnectionRequest> result = requests.findByStudentIdOrderByCreatedAtDesc(scope);
    Instant now = clock.instant();
    for (ConnectionRequest r : result) {
      if ("PENDING".equals(r.getStatus()) && vn.edufit.connection.application.policy.ConnectionTransitions.isDue(r.getExpiresAt(), now)) {
        if (expiryService != null) expiryService.expireRequest(r.getId(), now);
        r.expire(now);
      }
    }
    return result.stream().map(ConnectionRequestResult::from).toList();
  }

  @Transactional
  public List<TutorInboxResult> tutorInbox(CurrentUser actor) {
    requireRole(actor, "TUTOR");
    UUID tutorId = profile.getTutorIdByUserId(actor.getUserId());
    List<ConnectionRequest> result = requests.findByTutorIdOrderByCreatedAtDesc(tutorId);
    Instant now = clock.instant();
    for (ConnectionRequest r : result) {
      if ("PENDING".equals(r.getStatus()) && vn.edufit.connection.application.policy.ConnectionTransitions.isDue(r.getExpiresAt(), now)) {
        if (expiryService != null) expiryService.expireRequest(r.getId(), now);
        r.expire(now);
      }
    }
    if (result.isEmpty()) {
      return List.of();
    }
    List<UUID> goalIds = result.stream().map(ConnectionRequest::getGoalId).distinct().toList();
    List<UUID> sentByUserIds = result.stream().map(ConnectionRequest::getSentByUserId).distinct().toList();
    Map<UUID, ConnectionGoalDto> goalsMap = profile.findGoalsForConnection(goalIds);
    Map<UUID, UserSummaryView> usersMap = iam.findUserSummariesByIds(sentByUserIds);

    return result.stream().map(r -> {
      ConnectionGoalDto goal = goalsMap.get(r.getGoalId());
      if (goal == null) {
        throw new ResourceConflictException(ErrorCode.CONFLICT_DETECTED,
            "Không tìm thấy dữ liệu mục tiêu học tập liên quan");
      }
      UserSummaryView sender = usersMap.get(r.getSentByUserId());
      if (sender == null) {
        throw new ResourceConflictException(ErrorCode.CONFLICT_DETECTED,
            "Không tìm thấy thông tin người gửi yêu cầu");
      }
      String senderRole = sender.role() == AccountRole.PARENT ? "PARENT" : "STUDENT";
      return new TutorInboxResult(r.getId(), goal.goalType(), goal.subjectId(),
          goal.educationLevelId(), r.getDesiredSlots(), r.getMessage(), senderRole,
          r.getStatus(), r.getExpiresAt(), goal.deadline());
    }).toList();
  }

  private static void validatePageAndSize(int page, int size) {
    if (page < 0) throw new InvalidOperationException("Số trang không hợp lệ: page phải >= 0");
    if (size < 1 || size > 100) throw new InvalidOperationException("Kích thước trang không hợp lệ: size phải từ 1 đến 100");
  }

  @Transactional
  public PageResponse<InvitationResult> invitationsPage(CurrentUser actor, int page, int size) {
    requireRole(actor, "PARENT", "STUDENT");
    validatePageAndSize(page, size);
    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
    org.springframework.data.domain.Page<LinkInvitation> p = invitations.findScopedInvitations(actor.getUserId(), pageable);
    List<LinkInvitation> content = p.getContent();
    Instant now = clock.instant();
    for (LinkInvitation i : content) {
      if ("PENDING".equals(i.getStatus()) && vn.edufit.connection.application.policy.ConnectionTransitions.isDue(i.getExpiresAt(), now)) {
        if (expiryService != null) expiryService.expireInvitation(i.getId(), now);
        i.expire(now);
      }
    }
    if (content.isEmpty()) {
      return PageResponse.of(List.of(), page, size, p.getTotalElements());
    }
    List<UUID> inviterUserIds = content.stream().map(LinkInvitation::getInviterUserId).distinct().toList();
    Map<UUID, UserSummaryView> inviterSummaries = iam.findUserSummariesByIds(inviterUserIds);
    List<InvitationResult> items = content.stream().map(i -> {
      UserSummaryView inviter = inviterSummaries.get(i.getInviterUserId());
      String inviterName = inviter != null ? inviter.fullName() : null;
      String inviterRole = (inviter != null && inviter.role() != null) ? inviter.role().name() : null;
      return InvitationResult.from(i, inviterName, inviterRole);
    }).toList();
    return PageResponse.of(items, page, size, p.getTotalElements());
  }

  @Transactional(readOnly = true)
  public PageResponse<ParentStudentLinkResult> linksPage(CurrentUser actor, int page, int size) {
    requireRole(actor, "PARENT", "STUDENT");
    validatePageAndSize(page, size);
    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
    org.springframework.data.domain.Page<ParentStudentLink> p;
    if (actor.hasRole("PARENT")) {
      p = links.findByParentUserIdAndStatusConfirmed(actor.getUserId(), pageable);
    } else {
      UUID studentId = profile.getStudentIdByUserId(actor.getUserId());
      p = links.findByStudentIdAndStatusConfirmed(studentId, pageable);
    }
    List<ParentStudentLinkResult> items = p.getContent().stream().map(ParentStudentLinkResult::from).toList();
    return PageResponse.of(items, page, size, p.getTotalElements());
  }

  @Transactional(readOnly = true)
  public PageResponse<ParentStudentLinkResult> adminLinksPage(CurrentUser actor, String email, int page, int size) {
    requireRole(actor, "ADMIN");
    if (email == null || email.isBlank()) {
      throw new InvalidOperationException("Email không được để trống");
    }
    validatePageAndSize(page, size);

    Optional<UserSummaryView> userOpt = iam.findUserSummaryByEmail(email.trim());
    if (userOpt.isEmpty()) {
      return PageResponse.of(List.of(), page, size, 0L);
    }
    UserSummaryView targetUser = userOpt.get();
    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

    if (targetUser.role() == AccountRole.PARENT) {
      org.springframework.data.domain.Page<ParentStudentLink> p = links.findByParentUserIdAndStatusConfirmed(targetUser.id(), pageable);
      List<ParentStudentLinkResult> items = p.getContent().stream().map(ParentStudentLinkResult::from).toList();
      return PageResponse.of(items, page, size, p.getTotalElements());
    } else if (targetUser.role() == AccountRole.STUDENT) {
      Optional<StudentSummaryDto> studentOpt = profile.findStudentByUserId(targetUser.id());
      if (studentOpt.isEmpty()) {
        return PageResponse.of(List.of(), page, size, 0L);
      }
      UUID studentId = studentOpt.get().studentId();
      org.springframework.data.domain.Page<ParentStudentLink> p = links.findByStudentIdAndStatusConfirmed(studentId, pageable);
      List<ParentStudentLinkResult> items = p.getContent().stream().map(ParentStudentLinkResult::from).toList();
      return PageResponse.of(items, page, size, p.getTotalElements());
    } else {
      return PageResponse.of(List.of(), page, size, 0L);
    }
  }

  @Transactional
  public PageResponse<ConnectionRequestResult> requestsPage(CurrentUser actor, UUID studentId, int page, int size) {
    requireRole(actor, "STUDENT", "PARENT");
    validatePageAndSize(page, size);
    UUID scope = actor.hasRole("STUDENT") ? profile.getStudentIdByUserId(actor.getUserId()) : studentId;
    if (scope == null) throw new InvalidOperationException("Cần chỉ định học sinh");
    requireStudentAccess(actor, scope);
    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
    org.springframework.data.domain.Page<ConnectionRequest> p = requests.findByStudentIdScoped(scope, pageable);
    List<ConnectionRequest> content = p.getContent();
    Instant now = clock.instant();
    for (ConnectionRequest r : content) {
      if ("PENDING".equals(r.getStatus()) && vn.edufit.connection.application.policy.ConnectionTransitions.isDue(r.getExpiresAt(), now)) {
        if (expiryService != null) expiryService.expireRequest(r.getId(), now);
        r.expire(now);
      }
    }
    List<ConnectionRequestResult> items = content.stream().map(ConnectionRequestResult::from).toList();
    return PageResponse.of(items, page, size, p.getTotalElements());
  }

  @Transactional
  public PageResponse<TutorInboxResult> tutorInboxPage(CurrentUser actor, int page, int size) {
    requireRole(actor, "TUTOR");
    validatePageAndSize(page, size);
    UUID tutorId = profile.getTutorIdByUserId(actor.getUserId());
    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
    org.springframework.data.domain.Page<ConnectionRequest> p = requests.findByTutorIdScoped(tutorId, pageable);
    List<ConnectionRequest> content = p.getContent();
    Instant now = clock.instant();
    for (ConnectionRequest r : content) {
      if ("PENDING".equals(r.getStatus()) && vn.edufit.connection.application.policy.ConnectionTransitions.isDue(r.getExpiresAt(), now)) {
        if (expiryService != null) expiryService.expireRequest(r.getId(), now);
        r.expire(now);
      }
    }
    if (content.isEmpty()) {
      return PageResponse.of(List.of(), page, size, p.getTotalElements());
    }
    List<UUID> goalIds = content.stream().map(ConnectionRequest::getGoalId).distinct().toList();
    List<UUID> sentByUserIds = content.stream().map(ConnectionRequest::getSentByUserId).distinct().toList();
    Map<UUID, ConnectionGoalDto> goalsMap = profile.findGoalsForConnection(goalIds);
    Map<UUID, UserSummaryView> usersMap = iam.findUserSummariesByIds(sentByUserIds);

    List<TutorInboxResult> items = content.stream().map(r -> {
      ConnectionGoalDto goal = goalsMap.get(r.getGoalId());
      if (goal == null) {
        throw new ResourceConflictException(ErrorCode.CONFLICT_DETECTED,
            "Không tìm thấy dữ liệu mục tiêu học tập liên quan");
      }
      UserSummaryView sender = usersMap.get(r.getSentByUserId());
      if (sender == null) {
        throw new ResourceConflictException(ErrorCode.CONFLICT_DETECTED,
            "Không tìm thấy thông tin người gửi yêu cầu");
      }
      String senderRole = sender.role() == AccountRole.PARENT ? "PARENT" : "STUDENT";
      return new TutorInboxResult(r.getId(), goal.goalType(), goal.subjectId(),
          goal.educationLevelId(), r.getDesiredSlots(), r.getMessage(), senderRole,
          r.getStatus(), r.getExpiresAt(), goal.deadline());
    }).toList();
    return PageResponse.of(items, page, size, p.getTotalElements());
  }
}
