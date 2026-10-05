package vn.edufit.profile.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;

/**
 * JPA Entity ánh xạ bảng {@code tutor_availability_slot}.
 */
@Entity
@Table(name = "tutor_availability_slot")
public class TutorAvailabilitySlot {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "slot_id", updatable = false, nullable = false)
  private UUID slotId;

  @Column(name = "tutor_id", nullable = false)
  private UUID tutorId;

  @Column(name = "day_of_week", nullable = false)
  private Short dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  protected TutorAvailabilitySlot() {
    // JPA required
  }

  public TutorAvailabilitySlot(UUID tutorId, Short dayOfWeek, LocalTime startTime, LocalTime endTime) {
    this.tutorId = tutorId;
    this.dayOfWeek = dayOfWeek;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public UUID getSlotId() {
    return slotId;
  }

  public UUID getTutorId() {
    return tutorId;
  }

  public Short getDayOfWeek() {
    return dayOfWeek;
  }

  public void setDayOfWeek(Short dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }
}
