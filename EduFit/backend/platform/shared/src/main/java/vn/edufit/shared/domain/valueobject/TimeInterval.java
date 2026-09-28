package vn.edufit.shared.domain.valueobject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value Object đại diện cho một khoảng thời gian liên tục từ mốc bắt đầu (startTime) đến mốc kết thúc (endTime).
 *
 * <p>Mục đích thiết kế và liên kết yêu cầu nghiệp vụ:
 * <ul>
 *   <li><b>Quy tắc BR-42 (Conflict Policy):</b> Tính toán khoảng thời gian theo định dạng nửa khoảng mở
 *       {@code [start, end)}. Điểm kết thúc của buổi trước trùng với điểm bắt đầu của buổi sau thì <b>KHÔNG</b>
 *       bị coi là trùng lịch (ví dụ: Buổi 1 kết thúc lúc 09:00, Buổi 2 bắt đầu lúc 09:00 là hợp lệ).</li>
 *   <li><b>Yêu cầu NFR-17 (Double-booking Protection):</b> Cung cấp thuật toán kiểm tra giao nhau {@link #overlapsWith(TimeInterval)}
 *       chuẩn xác để các module {@code scheduling}, {@code profile} (lịch rảnh), và {@code discovery} (matching) dùng chung.</li>
 *   <li><b>Tính tự bảo vệ:</b> Ngăn chặn ngay từ đầu việc tạo ra khoảng thời gian có {@code endTime <= startTime}.</li>
 * </ul>
 *
 * @param startTime Thời điểm bắt đầu của khoảng thời gian (bao gồm điểm này - closed start).
 * @param endTime   Thời điểm kết thúc của khoảng thời gian (không bao gồm điểm này - open end).
 */
public record TimeInterval(LocalDateTime startTime, LocalDateTime endTime) {

  /**
   * Compact Constructor kiểm tra tính hợp lệ của khoảng thời gian.
   *
   * @throws NullPointerException     Nếu startTime hoặc endTime là null.
   * @throws IllegalArgumentException Nếu endTime không lớn hơn startTime (endTime <= startTime).
   */
  public TimeInterval {
    Objects.requireNonNull(startTime, "Thời gian bắt đầu (startTime) không được phép null");
    Objects.requireNonNull(endTime, "Thời gian kết thúc (endTime) không được phép null");

    if (!endTime.isAfter(startTime)) {
      throw new IllegalArgumentException(
          "Thời gian kết thúc phải lớn hơn thời gian bắt đầu. Bắt đầu: " + startTime + ", Kết thúc: " + endTime
      );
    }
  }

  /**
   * Phương thức tiện ích tạo khoảng thời gian từ mốc bắt đầu và độ dài tính theo phút.
   *
   * @param startTime       Thời điểm bắt đầu.
   * @param durationMinutes Thời lượng tính bằng phút (ví dụ: 90 phút cho 1 buổi học).
   * @return Đối tượng TimeInterval hợp lệ.
   */
  public static TimeInterval of(LocalDateTime startTime, long durationMinutes) {
    Objects.requireNonNull(startTime, "Thời gian bắt đầu không được null");
    if (durationMinutes <= 0) {
      throw new IllegalArgumentException("Thời lượng phút phải lớn hơn 0: " + durationMinutes);
    }
    return new TimeInterval(startTime, startTime.plusMinutes(durationMinutes));
  }

  /**
   * Kiểm tra xem khoảng thời gian hiện tại có bị giao (xung đột/overlap) với một khoảng thời gian khác hay không.
   *
   * <p>Theo quy tắc toán học nửa khoảng mở {@code [start, end)}:
   * Hai khoảng thời gian {@code [A.start, A.end)} và {@code [B.start, B.end)} giao nhau khi và chỉ khi:
   * <pre>{@code
   *   A.startTime < B.endTime VÀ A.endTime > B.startTime
   * }</pre>
   *
   * <p>Các trường hợp biên theo BR-42:
   * <ul>
   *   <li>A kết thúc trước khi B bắt đầu (A.end <= B.start) -> KHÔNG trùng (trả về false).</li>
   *   <li>A bắt đầu đúng lúc B kết thúc (A.start >= B.end) -> KHÔNG trùng (trả về false).</li>
   *   <li>A lấn vào một phần hoặc toàn bộ B -> TRÙNG (trả về true).</li>
   * </ul>
   *
   * @param other Khoảng thời gian khác đem ra kiểm tra.
   * @return {@code true} nếu hai khoảng thời gian bị xung đột, ngược lại {@code false}.
   */
  public boolean overlapsWith(TimeInterval other) {
    Objects.requireNonNull(other, "Khoảng thời gian đem so sánh không được phép null");
    return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
  }

  /**
   * Tính toán độ dài của khoảng thời gian theo phút.
   *
   * @return Số phút giữa startTime và endTime.
   */
  public long toDurationMinutes() {
    return Duration.between(startTime, endTime).toMinutes();
  }

  /**
   * Kiểm tra xem khoảng thời gian này có hoàn toàn nằm trước một mốc thời gian cụ thể hay không.
   *
   * @param point Mốc thời gian đem so sánh.
   * @return {@code true} nếu endTime <= point.
   */
  public boolean isFullyBefore(LocalDateTime point) {
    Objects.requireNonNull(point, "Mốc thời gian không được null");
    return !this.endTime.isAfter(point);
  }

  /**
   * Kiểm tra xem một mốc thời gian có nằm bên trong khoảng thời gian này hay không: {@code [start, end)}.
   *
   * @param point Mốc thời gian cần kiểm tra.
   * @return {@code true} nếu startTime <= point < endTime.
   */
  public boolean contains(LocalDateTime point) {
    Objects.requireNonNull(point, "Mốc thời gian không được null");
    return !point.isBefore(this.startTime) && point.isBefore(this.endTime);
  }
}
