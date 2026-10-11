package vn.edufit.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
class Notification {
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "notification_id") private UUID id;
  @Column(name = "user_id", nullable = false) private UUID userId;
  @Column(name = "about_student_id") private UUID studentId;
  @Column(name = "type", nullable = false, length = 60) private String type;
  @Column(name = "title", nullable = false, length = 200) private String title;
  @Column(name = "short_text", nullable = false, length = 500) private String text;
  @Column(name = "target_type", length = 40) private String targetType;
  @Column(name = "target_id") private UUID targetId;
  @Column(name = "is_read", nullable = false) private boolean read;
  @Column(name = "created_at", nullable = false) private Instant createdAt;

  protected Notification() {}
  Notification(UUID userId, UUID studentId, String type, String title, String text,
      String targetType, UUID targetId) {
    this.userId = userId;
    this.studentId = studentId;
    this.type = type;
    this.title = title;
    this.text = text;
    this.targetType = targetType;
    this.targetId = targetId;
    createdAt = Instant.now();
  }
}
