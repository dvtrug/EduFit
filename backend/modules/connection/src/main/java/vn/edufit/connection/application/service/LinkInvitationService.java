package vn.edufit.connection.application.service;

import java.time.Instant;
import java.time.Clock;
import vn.edufit.connection.application.dto.TransitionResult;
import vn.edufit.connection.application.policy.ConnectionTransitions;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.application.dto.ParentStudentLinkResult;
import vn.edufit.connection.application.port.ConnectionLock;
import vn.edufit.connection.infra.persistence.entity.LinkInvitation;
import vn.edufit.connection.infra.persistence.entity.ParentStudentLink;
import vn.edufit.connection.infra.persistence.repository.LinkInvitationRepository;
import vn.edufit.connection.infra.persistence.repository.ParentStudentLinkRepository;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.api.AccountStatus;
import vn.edufit.iam.api.IamFacade;
import vn.edufit.iam.api.dto.UserSummaryView;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.shared.auth.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import vn.edufit.connection.api.event.LinkInvitationChangedEvent;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.InvalidOperationException;

@Service
public class LinkInvitationService {
  private final Clock clock;
  private final LinkInvitationRepository invitations;
  private final ParentStudentLinkRepository links;
  private final IamFacade iam;
  private final ProfileFacade profile;
  private final ConnectionLock lock;
  private final InvitationQuotaService quotaService;
  private final ApplicationEventPublisher events;

  public LinkInvitationService(LinkInvitationRepository invitations, ParentStudentLinkRepository links,
      IamFacade iam, ProfileFacade profile, ConnectionLock lock, InvitationQuotaService quotaService,
      ApplicationEventPublisher events, Clock clock) {
    this.clock = clock;
    this.invitations = invitations;
    this.links = links;
    this.iam = iam;
    this.profile = profile;
    this.lock = lock;
    this.quotaService = quotaService;
    this.events = events;
  }

  private static void requireRole(CurrentUser actor, String... roles) {
    for (String role : roles) if (actor.hasRole(role)) return;
    throw new ForbiddenOperationException("Không có quyền thực hiện thao tác này");
  }

  private void requireActiveActor(CurrentUser actor) {
    AccountRole expected = actor.hasRole("PARENT") ? AccountRole.PARENT : AccountRole.STUDENT;
    iam.findUserSummaryById(actor.getUserId())
        .filter(user -> user.status() == AccountStatus.ACTIVE && user.role() == expected)
        .orElseThrow(() -> new ForbiddenOperationException("Tài khoản không còn quyền thực hiện thao tác"));
  }

  @Transactional
  public boolean invite(CurrentUser actor, String targetEmail) {
    requireRole(actor, "PARENT", "STUDENT");
    requireActiveActor(actor);
    var consumption = quotaService.consume(actor.getUserId());
    if (!consumption.allowed()) return false;
    Instant now = consumption.checkedAt();
    AccountRole opposite = actor.hasRole("PARENT") ? AccountRole.STUDENT : AccountRole.PARENT;
    Optional<UserSummaryView> target = iam.findUserSummaryByEmail(targetEmail)
        .filter(u -> u.status() == AccountStatus.ACTIVE && u.role() == opposite
            && !u.id().equals(actor.getUserId()));
    // Unknown and ineligible addresses intentionally have the same public result.
    if (target.isEmpty()) return true;
    UUID a = actor.getUserId(), b = target.get().id();
    lock.lockInvitationPair(a, b);
    List<LinkInvitation> history = invitations.findPendingPair(a, b);
    history.forEach(i -> i.expire(now));
    invitations.flush();
    if (history.stream().anyMatch(i -> "PENDING".equals(i.getStatus()))) return true;
    if (invitations.findFirstByInviterUserIdAndInvitedUserIdAndStatusOrderByReinviteAfterDesc(a, b, "DECLINED")
        .filter(i -> ConnectionTransitions.cooldownActive(i.getReinviteAfter(), now)).isPresent()) return true;
    UUID parent = actor.hasRole("PARENT") ? a : b;
    UUID studentUser = actor.hasRole("STUDENT") ? a : b;
    Optional<StudentSummaryDto> student = profile.findStudentByUserId(studentUser);
    if (student.isEmpty() || links.existsByParentUserIdAndStudentIdAndStatus(
        parent, student.get().studentId(), "CONFIRMED")) return true;
    LinkInvitation invitation = invitations.save(new LinkInvitation(a, b, now));
    publish(invitation, actor.getUserId(), student.get().studentId(), null, null,
        LinkInvitationChangedEvent.Cause.USER_ACTION, now);
    return true;
  }

  @Transactional
  public TransitionResult<ParentStudentLinkResult> respondInvitation(CurrentUser actor, UUID id, boolean accept) {
    requireRole(actor, "PARENT", "STUDENT");
    LinkInvitation initial = invitations.findById(id).orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    UUID a = initial.getInviterUserId(), b = initial.getInvitedUserId();
    if (!b.equals(actor.getUserId())) throw new ForbiddenOperationException("Không phải người nhận lời mời");
    UUID studentUser = actor.hasRole("STUDENT") ? b : a;
    Optional<StudentSummaryDto> studentScope = accept ? profile.findStudentByUserId(studentUser) : Optional.empty();
    studentScope.ifPresent(student -> lock.lockStudent(student.studentId()));
    lock.lockInvitationPair(a, b);
    LinkInvitation invitation = invitations.lockById(id).orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    requireActiveActor(actor);
    Instant now = clock.instant();
    invitation.expire(now);
    if (!"PENDING".equals(invitation.getStatus()))
      return TransitionResult.conflict(invitation.getStatus(), "Không còn ở trạng thái chờ xử lý");
    if (!accept) {
      invitation.decline(now);
      publish(invitation, actor.getUserId(), null, "PENDING", null,
          LinkInvitationChangedEvent.Cause.USER_ACTION, now);
      return TransitionResult.success(null);
    }
    UserSummaryView inviter = iam.findUserSummaryById(a)
        .orElseThrow(() -> new InvalidOperationException("Tài khoản gửi không còn hợp lệ"));
    AccountRole expected = actor.hasRole("PARENT") ? AccountRole.STUDENT : AccountRole.PARENT;
    if (inviter.status() != AccountStatus.ACTIVE || inviter.role() != expected)
      throw new InvalidOperationException("Tài khoản gửi không còn hợp lệ");
    UUID parent = actor.hasRole("PARENT") ? b : a;
    StudentSummaryDto student = profile.findStudentByUserId(studentUser)
        .filter(current -> current.userId().equals(studentUser)
            && studentScope.map(before -> before.studentId().equals(current.studentId())).orElse(false))
        .orElseThrow(() -> new InvalidOperationException("Hồ sơ học sinh không còn hợp lệ"));
    if (links.existsByParentUserIdAndStudentIdAndStatus(parent, student.studentId(), "CONFIRMED")) {
      invitation.withdraw(now);
      publish(invitation, null, student.studentId(), "PENDING", null,
          LinkInvitationChangedEvent.Cause.SYSTEM_ALREADY_LINKED, now);
      return TransitionResult.conflict("WITHDRAWN", "Liên kết đã tồn tại; lời mời đã được đóng");
    }
    invitation.accept(now);
    ParentStudentLink link = links.save(new ParentStudentLink(parent, student.studentId(), now));
    publish(invitation, actor.getUserId(), student.studentId(), "PENDING", link.getId(),
        LinkInvitationChangedEvent.Cause.USER_ACTION, now);
    return TransitionResult.success(ParentStudentLinkResult.from(link));
  }

  private void publish(LinkInvitation invitation, UUID actor, UUID student, String oldStatus, UUID linkId,
      LinkInvitationChangedEvent.Cause cause, Instant now) {
    events.publishEvent(new LinkInvitationChangedEvent(UUID.randomUUID(), invitation.getId(), actor, student,
        oldStatus, invitation.getStatus(), now, invitation.getInviterUserId(), invitation.getInvitedUserId(), linkId, cause));
  }

  @Transactional
  public TransitionResult<Void> withdrawInvitation(CurrentUser actor, UUID id) {
    requireRole(actor, "PARENT", "STUDENT");
    LinkInvitation initial = invitations.findById(id)
        .orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    UUID a = initial.getInviterUserId(), b = initial.getInvitedUserId();
    lock.lockInvitationPair(a, b);
    LinkInvitation invitation = invitations.lockById(id)
        .orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    if (!invitation.getInviterUserId().equals(actor.getUserId()))
      throw new ForbiddenOperationException("Không phải người gửi lời mời");
    requireActiveActor(actor);
    Instant now = clock.instant();
    invitation.expire(now);
    if (!"PENDING".equals(invitation.getStatus()))
      return TransitionResult.conflict(invitation.getStatus(), "Không còn ở trạng thái chờ xử lý");
    invitation.withdraw(now);
    return TransitionResult.success(null);
  }

}
