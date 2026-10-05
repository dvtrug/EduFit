package vn.edufit.profile.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record SetAvailabilitySlotsRequest(
    @NotNull(message = "Danh sách khung giờ không được null")
    @Valid
    List<SlotItem> slots
) {
  public record SlotItem(
      @NotNull(message = "Thứ trong tuần không được để trống")
      @Min(value = 1, message = "Thứ trong tuần phải từ 1 (Thứ Hai) đến 7 (Chủ Nhật)")
      @Max(value = 7, message = "Thứ trong tuần phải từ 1 (Thứ Hai) đến 7 (Chủ Nhật)")
      Short dayOfWeek,

      @NotNull(message = "Giờ bắt đầu không được để trống")
      LocalTime startTime,

      @NotNull(message = "Giờ kết thúc không được để trống")
      LocalTime endTime
  ) {}
}
