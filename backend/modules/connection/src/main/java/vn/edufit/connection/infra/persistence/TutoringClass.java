package vn.edufit.connection.infra.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "class")
public class TutoringClass {
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "class_id") private UUID id;
  @Column(name = "connection_request_id", nullable = false) private UUID connectionRequestId;
  @Column(name = "student_id", nullable = false) private UUID studentId;
  @Column(name = "tutor_id", nullable = false) private UUID tutorId;
  @Column(name = "goal_id", nullable = false) private UUID goalId;
  @Column(name = "status", nullable = false) private String status;
  @Column(name = "started_at", nullable = false) private Instant startedAt;
  @Column(name = "ended_at") private Instant endedAt;
  @Column(name = "end_reason") private String endReason;

  protected TutoringClass() {}
  public TutoringClass(ConnectionRequest request, Instant now) {
    connectionRequestId = request.getId();
    studentId = request.getStudentId();
    tutorId = request.getTutorId();
    goalId = request.getGoalId();
    status = "ACTIVE";
    startedAt = now;
  }
  public UUID getId() { return id; }
  public UUID getStudentId() { return studentId; }
  public UUID getTutorId() { return tutorId; }
  public UUID getGoalId() { return goalId; }
  public String getStatus() { return status; }
}
