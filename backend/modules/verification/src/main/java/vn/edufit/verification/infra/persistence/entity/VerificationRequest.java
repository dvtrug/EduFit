package vn.edufit.verification.infra.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity đại diện cho bảng {@code verification_request}.
 */
@Entity
@Table(name = "verification_request")
public class VerificationRequest {

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "request_id", updatable = false, nullable = false)
  private UUID requestId;

  @Column(name = "tutor_id", nullable = false)
  private UUID tutorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private Status status = Status.PENDING;

  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt = Instant.now();

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Credential> credentials = new ArrayList<>();

  protected VerificationRequest() {
    // JPA required
  }

  public VerificationRequest(UUID tutorId, Instant submittedAt) {
    this.tutorId = tutorId;
    this.status = Status.PENDING;
    this.submittedAt = submittedAt != null ? submittedAt : Instant.now();
  }

  public static VerificationRequest pending(UUID tutorId, Instant submittedAt) {
    return new VerificationRequest(tutorId, submittedAt);
  }

  public void addCredential(Credential credential) {
    if (credential != null) {
      this.credentials.add(credential);
      credential.assignRequest(this);
    }
  }

  public void approve(UUID reviewerUserId, Instant reviewedAt) {
    this.status = Status.APPROVED;
    this.reviewedBy = reviewerUserId;
    this.reviewedAt = reviewedAt != null ? reviewedAt : Instant.now();
    this.rejectionReason = null;
  }

  public void reject(UUID reviewerUserId, String reason, Instant reviewedAt) {
    this.status = Status.REJECTED;
    this.reviewedBy = reviewerUserId;
    this.reviewedAt = reviewedAt != null ? reviewedAt : Instant.now();
    this.rejectionReason = reason;
  }

  // Getters
  public UUID getRequestId() {
    return requestId;
  }

  public UUID getTutorId() {
    return tutorId;
  }

  public Status getStatus() {
    return status;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public UUID getReviewedBy() {
    return reviewedBy;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public List<Credential> getCredentials() {
    return Collections.unmodifiableList(credentials);
  }
}

