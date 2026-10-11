package vn.edufit.connection.application.service;

import java.time.Instant;
import java.time.Clock;
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
import vn.edufit.shared.notification.NotificationSink;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.ResourceConflictException;
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
  private final NotificationSink notifications;

  public LinkInvitationService(LinkInvitationRepository invitations, ParentStudentLinkRepository links,
      IamFacade iam, ProfileFacade profile, ConnectionLock lock, InvitationQuotaService quotaService,
      NotificationSink notifications, Clock clock) {
    this.clock = clock;
    this.invitations = invitations;
    this.links = links;
    this.iam = iam;
    this.profile = profile;
    this.lock = lock;
    this.quotaService = quotaService;
    this.notifications = notifications;
  }

  private static void requireRole(CurrentUser actor, String... roles) {
    for (String role : roles) if (actor.hasRole(role)) return;
    throw new ForbiddenOperationException("Không có quyền thực hiện thao tác này");
  }

  private static void pending(String status) {
    if (!"PENDING".equals(status)) throw new ResourceConflictException("Không còn ở trạng thái chờ xử lý");
  }

  @Transactional
  public boolean invite(CurrentUser actor, String targetEmail) {
    requireRole(actor, "PARENT", "STUDENT");
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
    List<LinkInvitation> history = invitations.findPair(a, b);
    history.forEach(i -> i.expire(now));
    invitations.flush();
    if (history.stream().anyMatch(i -> "PENDING".equals(i.getStatus()))) return true;
    if (history.stream().anyMatch(i -> i.getInviterUserId().equals(a) && "DECLINED".equals(i.getStatus())
        && ConnectionTransitions.cooldownActive(i.getReinviteAfter(), now))) return true;
    UUID parent = actor.hasRole("PARENT") ? a : b;
    UUID studentUser = actor.hasRole("STUDENT") ? a : b;
    Optional<StudentSummaryDto> student = profile.findStudentByUserId(studentUser);
    if (student.isEmpty() || links.existsByParentUserIdAndStudentIdAndStatus(
        parent, student.get().studentId(), "CONFIRMED")) return true;
    LinkInvitation invitation = invitations.save(new LinkInvitation(a, b, now));
    notifications.notify(b, student.get().studentId(), "LINK_INVITED", "Lời mời liên kết",
        "Bạn có lời mời liên kết mới", "LINK_INVITATION", invitation.getId());
    return true;
  }



  @Transactional(noRollbackFor = ResourceConflictException.class)
  public ParentStudentLinkResult respondInvitation(CurrentUser actor, UUID id, boolean accept) {
    requireRole(actor, "PARENT", "STUDENT");
    LinkInvitation initial = invitations.findById(id).orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    UUID a = initial.getInviterUserId(), b = initial.getInvitedUserId();
    if (!b.equals(actor.getUserId())) throw new ForbiddenOperationException("Không phải người nhận lời mời");
    UUID studentUser = actor.hasRole("STUDENT") ? b : a;
    Optional<StudentSummaryDto> studentScope = accept ? profile.findStudentByUserId(studentUser) : Optional.empty();
    studentScope.ifPresent(student -> lock.lockStudent(student.studentId()));
    lock.lockInvitationPair(a, b);
    LinkInvitation invitation = invitations.lockById(id).orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    Instant now = clock.instant();
    invitation.expire(now);
    pending(invitation.getStatus());
    if (!accept) {
      invitation.decline(now);
      notifications.notify(a, null, "LINK_DECLINED", "Lời mời đã bị từ chối",
          "Người nhận đã từ chối lời mời", "LINK_INVITATION", id);
      return null;
    }
    UserSummaryView inviter = iam.findUserSummaryById(a)
        .orElseThrow(() -> new InvalidOperationException("Tài khoản gửi không còn hợp lệ"));
    AccountRole expected = actor.hasRole("PARENT") ? AccountRole.STUDENT : AccountRole.PARENT;
    if (inviter.status() != AccountStatus.ACTIVE || inviter.role() != expected)
      throw new InvalidOperationException("Tài khoản gửi không còn hợp lệ");
    UUID parent = actor.hasRole("PARENT") ? b : a;
    StudentSummaryDto student = profile.findStudentByUserId(studentUser)
        .filter(current -> studentScope.map(before -> before.studentId().equals(current.studentId())).orElse(false))
        .orElseThrow(() -> new InvalidOperationException("Hồ sơ học sinh không còn hợp lệ"));
    if (links.existsByParentUserIdAndStudentIdAndStatus(parent, student.studentId(), "CONFIRMED"))
      throw new ResourceConflictException("Liên kết đã tồn tại");
    invitation.accept(now);
    ParentStudentLink link = links.save(new ParentStudentLink(parent, student.studentId(), now));
    notifications.notify(a, student.studentId(), "LINK_CONFIRMED", "Liên kết đã xác nhận",
        "Người nhận đã chấp nhận lời mời", "PARENT_STUDENT_LINK", link.getId());
    return ParentStudentLinkResult.from(link);
  }

  @Transactional(noRollbackFor = ResourceConflictException.class)
  public void withdrawInvitation(CurrentUser actor, UUID id) {
    requireRole(actor, "PARENT", "STUDENT");
    LinkInvitation initial = invitations.findById(id)
        .orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    UUID a = initial.getInviterUserId(), b = initial.getInvitedUserId();
    lock.lockInvitationPair(a, b);
    LinkInvitation invitation = invitations.lockById(id)
        .orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    if (!invitation.getInviterUserId().equals(actor.getUserId()))
      throw new ForbiddenOperationException("Không phải người gửi lời mời");
    Instant now = clock.instant();
    invitation.expire(now);
    pending(invitation.getStatus());
    invitation.withdraw(now);
  }

}
