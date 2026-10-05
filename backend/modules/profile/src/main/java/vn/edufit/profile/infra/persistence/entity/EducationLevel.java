package vn.edufit.profile.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity ánh xạ bảng {@code education_level}.
 */
@Entity
@Table(name = "education_level")
public class EducationLevel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "level_id")
  private Integer levelId;

  @Column(name = "name", nullable = false, length = 80, unique = true)
  private String name;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  protected EducationLevel() {
    // JPA required
  }

  public EducationLevel(String name, Integer sortOrder, Boolean isActive) {
    this.name = name;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
    this.isActive = isActive != null ? isActive : true;
  }

  public Integer getLevelId() {
    return levelId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean active) {
    isActive = active;
  }
}
