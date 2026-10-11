package vn.edufit.shared.notification;

import java.util.UUID;

public interface NotificationSink {
  void notify(UUID userId, UUID studentId, String type, String title, String text,
      String targetType, UUID targetId);
}
