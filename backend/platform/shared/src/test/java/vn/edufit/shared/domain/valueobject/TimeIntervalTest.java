package vn.edufit.shared.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Kiểm thử Value Object TimeInterval (Tuân thủ BR-42 & NFR-17)")
class TimeIntervalTest {

  private final LocalDateTime base = LocalDateTime.of(2026, 10, 1, 9, 0);

  @Nested
  @DisplayName("Kiểm tra tính hợp lệ khi khởi tạo (Self-Validation)")
  class ValidationTests {

    @Test
    @DisplayName("Khởi tạo thành công khi endTime lớn hơn startTime")
    void shouldCreateSuccessfullyWhenEndIsAfterStart() {
      LocalDateTime start = base;
      LocalDateTime end = base.plusHours(1);

      TimeInterval interval = new TimeInterval(start, end);

      assertEquals(start, interval.startTime());
      assertEquals(end, interval.endTime());
      assertEquals(60, interval.toDurationMinutes());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi endTime bằng startTime")
    void shouldThrowExceptionWhenEndEqualsStart() {
      assertThrows(IllegalArgumentException.class, () -> new TimeInterval(base, base));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi endTime đứng trước startTime")
    void shouldThrowExceptionWhenEndIsBeforeStart() {
      LocalDateTime start = base.plusHours(1);
      LocalDateTime end = base;

      assertThrows(IllegalArgumentException.class, () -> new TimeInterval(start, end));
    }

    @Test
    @DisplayName("Ném NullPointerException khi tham số là null")
    void shouldThrowExceptionWhenParametersAreNull() {
      assertThrows(NullPointerException.class, () -> new TimeInterval(null, base));
      assertThrows(NullPointerException.class, () -> new TimeInterval(base, null));
    }
  }

  @Nested
  @DisplayName("Kiểm tra quy tắc xung đột lịch học (BR-42: Nửa khoảng mở [start, end))")
  class OverlapPolicyTests {

    // Khoảng gốc: 09:00 -> 10:30
    private final TimeInterval baseInterval = new TimeInterval(
        LocalDateTime.of(2026, 10, 1, 9, 0),
        LocalDateTime.of(2026, 10, 1, 10, 30)
    );

    @Test
    @DisplayName("Trường hợp 1 (Biên BR-42): Liền kề nhau - A kết thúc đúng lúc B bắt đầu -> KHÔNG TRÙNG")
    void shouldNotOverlapWhenOneEndsExactlyWhenAnotherStarts() {
      // 10:30 -> 12:00 (nối tiếp ngay sau baseInterval)
      TimeInterval adjacentAfter = new TimeInterval(
          LocalDateTime.of(2026, 10, 1, 10, 30),
          LocalDateTime.of(2026, 10, 1, 12, 0)
      );

      // 07:30 -> 09:00 (nối tiếp ngay trước baseInterval)
      TimeInterval adjacentBefore = new TimeInterval(
          LocalDateTime.of(2026, 10, 1, 7, 30),
          LocalDateTime.of(2026, 10, 1, 9, 0)
      );

      assertFalse(baseInterval.overlapsWith(adjacentAfter), "Liền kề sau không được coi là trùng lịch");
      assertFalse(adjacentAfter.overlapsWith(baseInterval), "Tính chất giao hoán: B so với A cũng không trùng");
      assertFalse(baseInterval.overlapsWith(adjacentBefore), "Liền kề trước không được coi là trùng lịch");
    }

    @Test
    @DisplayName("Trường hợp 2: Lấn đuôi - Bắt đầu trước khi khoảng cũ kết thúc -> TRÙNG")
    void shouldOverlapWhenPartiallyOverlappingEnd() {
      // 10:00 -> 11:30 (lấn vào 30 phút cuối của 09:00 - 10:30)
      TimeInterval overlapping = new TimeInterval(
          LocalDateTime.of(2026, 10, 1, 10, 0),
          LocalDateTime.of(2026, 10, 1, 11, 30)
      );

      assertTrue(baseInterval.overlapsWith(overlapping));
      assertTrue(overlapping.overlapsWith(baseInterval));
    }

    @Test
    @DisplayName("Trường hợp 3: Nằm trọn bên trong -> TRÙNG")
    void shouldOverlapWhenFullyContainedInside() {
      // 09:30 -> 10:00 (nằm hoàn toàn trong 09:00 - 10:30)
      TimeInterval inside = new TimeInterval(
          LocalDateTime.of(2026, 10, 1, 9, 30),
          LocalDateTime.of(2026, 10, 1, 10, 0)
      );

      assertTrue(baseInterval.overlapsWith(inside));
      assertTrue(inside.overlapsWith(baseInterval));
    }

    @Test
    @DisplayName("Trường hợp 4: Bao trùm hoàn toàn -> TRÙNG")
    void shouldOverlapWhenEnclosingAnother() {
      // 08:30 -> 11:00 (bao trùm cả 09:00 - 10:30)
      TimeInterval enclosing = new TimeInterval(
          LocalDateTime.of(2026, 10, 1, 8, 30),
          LocalDateTime.of(2026, 10, 1, 11, 0)
      );

      assertTrue(baseInterval.overlapsWith(enclosing));
      assertTrue(enclosing.overlapsWith(baseInterval));
    }

    @Test
    @DisplayName("Trường hợp 5: Cách xa hoàn toàn -> KHÔNG TRÙNG")
    void shouldNotOverlapWhenCompletelyDisjoint() {
      // 14:00 -> 15:30 (buổi chiều, không liên quan buổi sáng)
      TimeInterval afternoon = new TimeInterval(
          LocalDateTime.of(2026, 10, 1, 14, 0),
          LocalDateTime.of(2026, 10, 1, 15, 30)
      );

      assertFalse(baseInterval.overlapsWith(afternoon));
    }
  }

  @Nested
  @DisplayName("Kiểm tra các hàm tiện ích khác")
  class UtilityTests {

    @Test
    @DisplayName("Kiểm tra điểm thời gian nằm trong khoảng [start, end)")
    void shouldCheckContainsCorrectly() {
      TimeInterval interval = new TimeInterval(base, base.plusHours(2)); // 09:00 - 11:00

      assertTrue(interval.contains(base), "Chứa điểm bắt đầu (09:00)");
      assertTrue(interval.contains(base.plusHours(1)), "Chứa điểm giữa (10:00)");
      assertFalse(interval.contains(base.plusHours(2)), "KHÔNG chứa điểm kết thúc (11:00)");
      assertFalse(interval.contains(base.minusMinutes(1)), "Không chứa điểm trước đó");
    }
  }
}
