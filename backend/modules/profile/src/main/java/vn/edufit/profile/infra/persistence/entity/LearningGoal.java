package vn.edufit.profile.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA Entity ánh xạ bảng {@code learning_goal}.
 */
@Entity
@Table(name = "learning_goal")
public class LearningGoal {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "goal_id", updatable = false, nullable = false)
  private UUID goalId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "subject_id", nullable = false)
  private Integer subjectId;

  @Column(name = "current_level", nullable = false)
  private Short currentLevel;

  @Column(name = "desired_level", nullable = false)
  private Short desiredLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "goal_type", nullable = false, length = 30)
  private GoalType goalType;

  @Column(name = "deadline", nullable = false)
  private LocalDate deadline;

  @Enumerated(EnumType.STRING)
  @Column(name = "mode", nullable = false, length = 20)
  private TeachingMode mode;

  @Column(name = "area", length = 100)
  private String area;

  @Column(name = "budget_min")
  private Long budgetMin;

  @Column(name = "budget_max", nullable = false)
  private Long budgetMax;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private GoalStatus status = GoalStatus.ACTIVE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected LearningGoal() {
    // JPA required
  }

  public LearningGoal(
      UUID studentId,
      Integer subjectId,
      Short currentLevel,
      Short desiredLevel,
      GoalType goalType,
      LocalDate deadline,
      TeachingMode mode,
      Long budgetMax
  ) {
    this.studentId = studentId;
    this.subjectId = subjectId;
    this.currentLevel = currentLevel;
    this.desiredLevel = desiredLevel;
    this.goalType = goalType;
    this.deadline = deadline;
    this.mode = mode;
    this.budgetMax = budgetMax;
    this.status = GoalStatus.ACTIVE;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public UUID getGoalId() {
    return goalId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public Integer getSubjectId() {
    return subjectId;
  }

  public void setSubjectId(Integer subjectId) {
    this.subjectId = subjectId;
  }

  public Short getCurrentLevel() {
    return currentLevel;
  }

  public void setCurrentLevel(Short currentLevel) {
    this.currentLevel = currentLevel;
  }

  public Short getDesiredLevel() {
    return desiredLevel;
  }

  public void setDesiredLevel(Short desiredLevel) {
    this.desiredLevel = desiredLevel;
  }

  public GoalType getGoalType() {
    return goalType;
  }

  public void setGoalType(GoalType goalType) {
    this.goalType = goalType;
  }

  public LocalDate getDeadline() {
    return deadline;
  }

  public void setDeadline(LocalDate deadline) {
    this.deadline = deadline;
  }

  public TeachingMode getMode() {
    return mode;
  }

  public void setMode(TeachingMode mode) {
    this.mode = mode;
  }

  public String getArea() {
    return area;
  }

  public void setArea(String area) {
    this.area = area;
  }

  public Long getBudgetMin() {
    return budgetMin;
  }

  public void setBudgetMin(Long budgetMin) {
    this.budgetMin = budgetMin;
  }

  public Long getBudgetMax() {
    return budgetMax;
  }

  public void setBudgetMax(Long budgetMax) {
    this.budgetMax = budgetMax;
  }

  public GoalStatus getStatus() {
    return status;
  }

  public void setStatus(GoalStatus status) {
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
