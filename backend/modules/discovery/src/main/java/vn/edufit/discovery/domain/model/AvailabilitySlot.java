package vn.edufit.discovery.domain.model;

import java.time.LocalTime;

public record AvailabilitySlot(short dayOfWeek, LocalTime startTime, LocalTime endTime) {

  public boolean overlaps(AvailabilitySlot other) {
    return dayOfWeek == other.dayOfWeek
        && startTime.isBefore(other.endTime)
        && endTime.isAfter(other.startTime);
  }
}
