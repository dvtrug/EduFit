package vn.edufit.notification;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import vn.edufit.shared.notification.NotificationSink;

interface NotificationRepository extends JpaRepository<Notification, UUID> {}

@Service
class JpaNotificationSink implements NotificationSink {
  private final NotificationRepository repository;
  JpaNotificationSink(NotificationRepository repository) { this.repository = repository; }

  @Override
  public void notify(UUID userId, UUID studentId, String type, String title, String text,
      String targetType, UUID targetId) {
    repository.save(new Notification(userId, studentId, type, title, text, targetType, targetId));
  }
}
