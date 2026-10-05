package vn.edufit.profile.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity đại diện cho bảng {@code tutor_profile}.
 */
@Entity
@Table(name = "tutor_profile")
public class TutorProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "tutor_id", updatable = false, nullable = false)
  private UUID tutorId;

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "headline", length = 200)
  private String headline;

  @Column(name = "bio", columnDefinition = "TEXT")
  private String bio;

  @Enumerated(EnumType.STRING)
  @Column(name = "teaching_mode", nullable = false, length = 20)
  private TeachingMode teachingMode;

  @Column(name = "area", length = 100)
  private String area;

  @Column(name = "price_per_session", nullable = false)
  private Long pricePerSession;

  @Column(name = "experience_years")
  private Short experienceYears;

  @Column(name = "teaching_method", columnDefinition = "TEXT")
  private String teachingMethod;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private TutorStatus status = TutorStatus.DRAFT;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Column(name = "rating_avg", precision = 3, scale = 2)
  private BigDecimal ratingAvg;

  @Column(name = "review_count", nullable = false)
  private Integer reviewCount = 0;

  @Version
  @Column(name = "version", nullable = false)
  private Integer version = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected TutorProfile() {
    // JPA required
  }

  public TutorProfile(
      UUID userId,
      String displayName,
      TeachingMode teachingMode,
      Long pricePerSession
  ) {
    this.userId = userId;
    this.displayName = displayName;
    this.teachingMode = teachingMode;
    this.pricePerSession = pricePerSession;
    this.status = TutorStatus.DRAFT;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public void markPendingVerification() {
    this.status = TutorStatus.PENDING_VERIFICATION;
    this.updatedAt = Instant.now();
  }

  public void markVerified(Instant verifiedAt) {
    this.status = TutorStatus.VERIFIED;
    this.verifiedAt = verifiedAt != null ? verifiedAt : Instant.now();
    this.updatedAt = Instant.now();
  }

  public void markRejected() {
    this.status = TutorStatus.UNVERIFIED;
    this.verifiedAt = null;
    this.updatedAt = Instant.now();
  }

  // Getters & Setters
  public UUID getTutorId() {
    return tutorId;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getHeadline() {
    return headline;
  }

  public void setHeadline(String headline) {
    this.headline = headline;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public TeachingMode getTeachingMode() {
    return teachingMode;
  }

  public void setTeachingMode(TeachingMode teachingMode) {
    this.teachingMode = teachingMode;
  }

  public String getArea() {
    return area;
  }

  public void setArea(String area) {
    this.area = area;
  }

  public Long getPricePerSession() {
    return pricePerSession;
  }

  public void setPricePerSession(Long pricePerSession) {
    this.pricePerSession = pricePerSession;
  }

  public Short getExperienceYears() {
    return experienceYears;
  }

  public void setExperienceYears(Short experienceYears) {
    this.experienceYears = experienceYears;
  }

  public String getTeachingMethod() {
    return teachingMethod;
  }

  public void setTeachingMethod(String teachingMethod) {
    this.teachingMethod = teachingMethod;
  }

  public TutorStatus getStatus() {
    return status;
  }

  public void setStatus(TutorStatus status) {
    this.status = status;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public BigDecimal getRatingAvg() {
    return ratingAvg;
  }

  public Integer getReviewCount() {
    return reviewCount;
  }

  public Integer getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}

