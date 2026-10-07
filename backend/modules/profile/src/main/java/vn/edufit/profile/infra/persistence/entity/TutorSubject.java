package vn.edufit.profile.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * JPA Entity ánh xạ bảng {@code tutor_subject}.
 */
@Entity
@Table(
    name = "tutor_subject",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_tutor_subject",
            columnNames = {"tutor_id", "subject_id", "education_level_id"}
        )
    }
)
public class TutorSubject {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "tutor_subject_id", updatable = false, nullable = false)
  private UUID tutorSubjectId;

  @Column(name = "tutor_id", nullable = false)
  private UUID tutorId;

  @Column(name = "subject_id", nullable = false)
  private Integer subjectId;

  @Column(name = "education_level_id", nullable = false)
  private Integer educationLevelId;

  protected TutorSubject() {
    // JPA required
  }

  public TutorSubject(UUID tutorId, Integer subjectId, Integer educationLevelId) {
    this.tutorId = tutorId;
    this.subjectId = subjectId;
    this.educationLevelId = educationLevelId;
  }

  public UUID getTutorSubjectId() {
    return tutorSubjectId;
  }

  public UUID getTutorId() {
    return tutorId;
  }

  public Integer getSubjectId() {
    return subjectId;
  }

  public Integer getEducationLevelId() {
    return educationLevelId;
  }
}
