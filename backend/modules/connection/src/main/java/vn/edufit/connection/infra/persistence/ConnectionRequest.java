package vn.edufit.connection.infra.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

@Entity
@Table(name = "connection_request")
public class ConnectionRequest {
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "request_id") private UUID id;
  @Column(name = "student_id", nullable = false) private UUID studentId;
  @Column(name = "tutor_id", nullable = false) private UUID tutorId;
  @Column(name = "goal_id", nullable = false) private UUID goalId;
  @Column(name = "sent_by_user_id", nullable = false) private UUID sentByUserId;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name = "desired_slots", columnDefinition = "jsonb") private List<String> desiredSlots;
  @Column(name = "message", length = 500) private String message;
  @Column(name = "status", nullable = false) private String status;
  @Column(name = "reject_reason", length = 300) private String rejectReason;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "expires_at", nullable = false) private Instant expiresAt;
  @Column(name = "responded_at") private Instant respondedAt;
  @Version @Column(name = "version", nullable = false) private Long version;

  protected ConnectionRequest() {}
  public ConnectionRequest(UUID studentId, UUID tutorId, UUID goalId, UUID sender,
      List<String> desiredSlots, String message, Instant now) {
    this.studentId = studentId;
    this.tutorId = tutorId;
    this.goalId = goalId;
    sentByUserId = sender;
    this.desiredSlots = desiredSlots;
    this.message = message;
    status = "PENDING";
    createdAt = now;
    expiresAt = now.plus(java.time.Duration.ofDays(7));
  }
  public void expire(Instant now) {
    if ("PENDING".equals(status) && !expiresAt.isAfter(now)) status = "EXPIRED";
  }
  public void cancel(Instant now) { status = "CANCELLED"; respondedAt = now; }
  public void accept(Instant now) { status = "ACCEPTED"; respondedAt = now; }
  public void reject(String reason, Instant now) {
    status = "REJECTED"; rejectReason = reason; respondedAt = now;
  }
  public UUID getId() { return id; }
  public UUID getStudentId() { return studentId; }
  public UUID getTutorId() { return tutorId; }
  public UUID getGoalId() { return goalId; }
  public UUID getSentByUserId() { return sentByUserId; }
  public String getStatus() { return status; }
  public List<String> getDesiredSlots() { return desiredSlots; }
  public String getMessage() { return message; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public String getRejectReason() { return rejectReason; }
}
