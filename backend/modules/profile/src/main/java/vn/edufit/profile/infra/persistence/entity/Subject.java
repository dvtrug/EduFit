package vn.edufit.profile.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity ánh xạ bảng {@code subject}.
 */
@Entity
@Table(name = "subject")
public class Subject {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "subject_id")
  private Integer subjectId;

  @Column(name = "name", nullable = false, length = 100, unique = true)
  private String name;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  protected Subject() {
    // JPA required
  }

  public Subject(String name, Boolean isActive) {
    this.name = name;
    this.isActive = isActive != null ? isActive : true;
  }

  public Integer getSubjectId() {
    return subjectId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean active) {
    isActive = active;
  }
}
