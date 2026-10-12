package vn.edufit.profile.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.util.List;

public record SetGoalAvailabilityRequest(
    @NotNull @Size(max = 100) @Valid List<@NotNull Slot> slots) {
  public record Slot(
      @NotNull @Min(1) @Max(7) Short dayOfWeek,
      @NotNull LocalTime startTime,
      @NotNull LocalTime endTime) {
    @AssertTrue(message = "Khung giờ phải bắt đầu trước khi kết thúc trong cùng ngày")
    public boolean isTimeOrderValid() {
      return startTime == null || endTime == null || startTime.isBefore(endTime);
    }

    @AssertTrue(message = "Khung giờ phải có độ chính xác phút")
    public boolean isMinuteAligned() {
      return (startTime == null || (startTime.getSecond() == 0 && startTime.getNano() == 0))
          && (endTime == null || (endTime.getSecond() == 0 && endTime.getNano() == 0));
    }
  }
}
