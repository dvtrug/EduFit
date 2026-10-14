package vn.edufit.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.event.ConnectionRequestChangedEvent;
import vn.edufit.connection.api.event.LinkInvitationChangedEvent;
import vn.edufit.connection.api.event.ParentStudentLinkChangedEvent;

/** Internal mandatory writer; never starts an independent notification transaction. */
@Service
class ConnectionNotificationWriter {
  private final NotificationRepository repository;
  ConnectionNotificationWriter(NotificationRepository repository) { this.repository = repository; }

  @Transactional(propagation = Propagation.MANDATORY)
  public void invitationCreated(LinkInvitationChangedEvent event) {
    repository.save(new Notification(event.invitedUserId(), event.studentId(), "LINK_INVITED",
        "Lời mời liên kết", "Bạn có lời mời liên kết mới", "LINK_INVITATION", event.aggregateId(), event.occurredAt()));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void invitationAccepted(LinkInvitationChangedEvent event) {
    repository.save(new Notification(event.inviterUserId(), event.studentId(), "LINK_CONFIRMED",
        "Liên kết đã xác nhận", "Người nhận đã chấp nhận lời mời", "PARENT_STUDENT_LINK",
        java.util.Objects.requireNonNull(event.linkId()), event.occurredAt()));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void invitationDeclined(LinkInvitationChangedEvent event) {
    repository.save(new Notification(event.inviterUserId(), null, "LINK_DECLINED",
        "Lời mời đã bị từ chối", "Người nhận đã từ chối lời mời", "LINK_INVITATION", event.aggregateId(), event.occurredAt()));
  }
  @Transactional(propagation = Propagation.MANDATORY)
  public void linkRevoked(ParentStudentLinkChangedEvent event) {
    // Parent can retain this minimal account notification after consent ends.
    repository.save(new Notification(event.parentUserId(), null, "LINK_REVOKED",
        "Liên kết đã thu hồi", "Quyền truy cập hồ sơ học sinh đã kết thúc", "PARENT_STUDENT_LINK",
        event.aggregateId(), event.occurredAt()));
    if (!event.studentUserId().equals(event.actorUserId()))
      repository.save(new Notification(event.studentUserId(), event.studentId(), "LINK_REVOKED",
          "Liên kết đã thu hồi", "Liên kết phụ huynh đã được thu hồi", "PARENT_STUDENT_LINK",
          event.aggregateId(), event.occurredAt()));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void requestCreated(ConnectionRequestChangedEvent event) {
    repository.save(new Notification(event.tutorUserId(), event.studentId(), "CONNECTION_REQUESTED",
        "Yêu cầu kết nối mới", "Bạn có yêu cầu dạy học mới", "CONNECTION_REQUEST",
        event.aggregateId(), event.occurredAt()));
    if (!event.studentUserId().equals(event.sentByUserId())) {
      repository.save(new Notification(event.studentUserId(), event.studentId(), "CONNECTION_REQUESTED",
          "Yêu cầu kết nối mới", "Phụ huynh đã gửi yêu cầu kết nối gia sư", "CONNECTION_REQUEST",
          event.aggregateId(), event.occurredAt()));
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void requestCancelled(ConnectionRequestChangedEvent event) {
    repository.save(new Notification(event.tutorUserId(), event.studentId(), "CONNECTION_CANCELLED",
        "Yêu cầu đã bị hủy", "Yêu cầu kết nối đã bị hủy", "CONNECTION_REQUEST",
        event.aggregateId(), event.occurredAt()));
    if (!event.studentUserId().equals(event.actorUserId())) {
      repository.save(new Notification(event.studentUserId(), event.studentId(), "CONNECTION_CANCELLED",
          "Yêu cầu đã bị hủy", "Yêu cầu kết nối đã bị hủy", "CONNECTION_REQUEST",
          event.aggregateId(), event.occurredAt()));
    }
  }
}
